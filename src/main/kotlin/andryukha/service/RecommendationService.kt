package andryukha.service

import andryukha.domain.Recommendation
import andryukha.domain.RecommendationStrategy
import andryukha.persistence.VisitRepository
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
class RecommendationService(
    private val repository: VisitRepository,
    private val strategy: RecommendationStrategy,
    private val clock: Clock,
    private val metrics: MeterRegistry,
) {
    @Transactional(readOnly = true)
    fun preview(asOf: LocalDate?): List<Recommendation> {
        val date = asOf ?: LocalDate.now(clock)
        require(!date.isAfter(LocalDate.now(clock))) { "Дата расчёта не может быть в будущем." }
        return strategy.recommend(repository.findAllByVisitedOnLessThanEqual(date).map { it.toDomain() }, date)
            .also { results ->
                results.groupingBy { it.status }.eachCount().forEach { (status, count) ->
                    metrics.counter("andryukha.recommendations.evaluated", "status", status.name).increment(count.toDouble())
                }
            }
    }
}
