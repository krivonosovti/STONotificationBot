package andryukha.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

enum class RecommendationStatus {
    READY, NOT_DUE, INSUFFICIENT_HISTORY, INCONSISTENT_HISTORY,
    AMBIGUOUS_OWNER, INVALID_ODOMETER, OVERDUE_REVIEW,
}

data class Recommendation(
    val customerId: String,
    val vehicleId: String,
    val serviceType: ServiceType = ServiceType.MAINTENANCE,
    val status: RecommendationStatus,
    val dueOn: LocalDate? = null,
    val intervalDays: Long? = null,
    val historyCount: Int,
    val reason: String,
    val ruleVersion: String = "maintenance-history-v1",
)

fun interface RecommendationStrategy {
    fun recommend(history: List<Visit>, asOf: LocalDate): List<Recommendation>
}

/** Predicts a return date from past maintenance visits, never a mechanical diagnosis. */
class HistoryBasedRecommendationStrategy(
    private val leadDays: Long = 14,
    private val overdueReviewDays: Long = 60,
    private val minimumIntervalDays: Long = 14,
    private val maximumIntervalDays: Long = 730,
    private val maximumIntervalRatio: Double = 3.0,
) : RecommendationStrategy {
    override fun recommend(history: List<Visit>, asOf: LocalDate): List<Recommendation> = history
        .filter { it.status == VisitStatus.COMPLETED && !it.visitedOn.isAfter(asOf) }
        .groupBy { it.vehicleId }
        .toSortedMap()
        .flatMap { (_, vehicleVisits) ->
            val ambiguousOwner = vehicleVisits.map { it.customerId }.distinct().size > 1
            vehicleVisits.groupBy { it.customerId }.toSortedMap().map { (_, customerVisits) ->
                evaluate(customerVisits, asOf, ambiguousOwner)
            }
        }

    private fun evaluate(visits: List<Visit>, asOf: LocalDate, ambiguousOwner: Boolean): Recommendation {
        val first = visits.first()
        val maintenance = visits.filter { ServiceType.MAINTENANCE in it.services }
            .distinctBy { it.visitedOn }.sortedBy { it.visitedOn }.takeLast(5)
        fun result(status: RecommendationStatus, reason: String, due: LocalDate? = null, interval: Long? = null) =
            Recommendation(first.customerId, first.vehicleId, status = status, dueOn = due,
                intervalDays = interval, historyCount = maintenance.size, reason = reason)

        if (ambiguousOwner) return result(RecommendationStatus.AMBIGUOUS_OWNER,
            "В истории машины несколько клиентов: нужно уточнить текущего владельца.")
        val odometers = visits.filter { it.odometerKm != null }.sortedBy { it.visitedOn }
        val conflictingSameDay = odometers.groupBy { it.visitedOn }.values.any { day ->
            day.map { it.odometerKm }.distinct().size > 1
        }
        if (conflictingSameDay || odometers.zipWithNext().any { (a, b) -> b.odometerKm!! < a.odometerKm!! })
            return result(RecommendationStatus.INVALID_ODOMETER, "Пробег противоречив: нужна проверка данных.")
        if (maintenance.size < 3) return result(RecommendationStatus.INSUFFICIENT_HISTORY,
            "Нужно минимум три завершённых ТО в разные даты.")
        val intervals = maintenance.zipWithNext { a, b -> ChronoUnit.DAYS.between(a.visitedOn, b.visitedOn) }.sorted()
        if (intervals.any { it !in minimumIntervalDays..maximumIntervalDays } ||
            intervals.last().toDouble() / intervals.first() > maximumIntervalRatio)
            return result(RecommendationStatus.INCONSISTENT_HISTORY, "Интервалы ТО нестабильны: решение за мастером.")
        val middle = intervals.size / 2
        val median = if (intervals.size % 2 == 1) intervals[middle]
            else ((intervals[middle - 1] + intervals[middle]) / 2.0).roundToLong()
        val due = maintenance.last().visitedOn.plusDays(median)
        return when {
            asOf.isBefore(due.minusDays(leadDays)) -> result(RecommendationStatus.NOT_DUE,
                "Срок напоминания ещё не наступил.", due, median)
            asOf.isAfter(due.plusDays(overdueReviewDays)) -> result(RecommendationStatus.OVERDUE_REVIEW,
                "Предполагаемый срок давно прошёл: нужно уточнить актуальность.", due, median)
            else -> result(RecommendationStatus.READY,
                "Оценка по медиане интервалов прошлых ТО. Мастер должен подтвердить рекомендацию.", due, median)
        }
    }
}
