package andryukha

import andryukha.domain.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HistoryBasedRecommendationStrategyTest {
    private val strategy = HistoryBasedRecommendationStrategy()
    private val today = LocalDate.parse("2026-10-29")
    private fun visit(date: String, id: String = date, km: Int? = null) = Visit(id, "customer-1", "car-1",
        LocalDate.parse(date), km, setOf(ServiceType.MAINTENANCE), VisitStatus.COMPLETED)
    private fun history() = listOf(visit("2026-02-01"), visit("2026-05-02"), visit("2026-07-31"))

    @Test fun `three regular visits produce a maintenance reminder`() {
        val result = strategy.recommend(history(), today).single()
        assertThat(result.status).isEqualTo(RecommendationStatus.READY)
        assertThat(result.dueOn).isEqualTo(today)
        assertThat(result.intervalDays).isEqualTo(90)
    }
    @Test fun `one or two visits do not establish a personal interval`() {
        assertThat(strategy.recommend(history().take(2), today).single().status)
            .isEqualTo(RecommendationStatus.INSUFFICIENT_HISTORY)
    }
    @Test fun `duplicates on the same day do not create extra history`() {
        val visits = listOf(visit("2026-05-02", "a"), visit("2026-05-02", "b"), visit("2026-07-31", "c"))
        assertThat(strategy.recommend(visits, today).single().historyCount).isEqualTo(2)
    }
    @Test fun `future and cancelled visits do not influence historical preview`() {
        val extra = listOf(visit("2027-01-01"), visit("2026-09-01").copy(status = VisitStatus.CANCELLED))
        assertThat(strategy.recommend(history() + extra, today)).isEqualTo(strategy.recommend(history(), today))
    }
    @Test fun `too early and very overdue reminders are held`() {
        assertThat(strategy.recommend(history(), today.minusDays(15)).single().status).isEqualTo(RecommendationStatus.NOT_DUE)
        assertThat(strategy.recommend(history(), today.plusDays(61)).single().status).isEqualTo(RecommendationStatus.OVERDUE_REVIEW)
    }
    @Test fun `reminder window includes both boundary dates`() {
        assertThat(strategy.recommend(history(), today.minusDays(14)).single().status).isEqualTo(RecommendationStatus.READY)
        assertThat(strategy.recommend(history(), today.plusDays(60)).single().status).isEqualTo(RecommendationStatus.READY)
    }
    @Test fun `multiple customers for one vehicle require ownership clarification`() {
        val extra = visit("2026-09-01").copy(customerId = "customer-2", services = setOf(ServiceType.OTHER))
        assertThat(strategy.recommend(history() + extra, today)).allMatch { it.status == RecommendationStatus.AMBIGUOUS_OWNER }
    }
    @Test fun `decreasing odometer requires review`() {
        val data = history().mapIndexed { i, v -> v.copy(odometerKm = 100_000 - i * 10_000) }
        assertThat(strategy.recommend(data, today).single().status).isEqualTo(RecommendationStatus.INVALID_ODOMETER)
    }
    @Test fun `conflicting odometers on the same day require review`() {
        val data = history() + listOf(visit("2026-09-01", "x", 120000), visit("2026-09-01", "y", 130000))
        assertThat(strategy.recommend(data, today).single().status).isEqualTo(RecommendationStatus.INVALID_ODOMETER)
    }
    @Test fun `brake jobs do not count as maintenance`() {
        val data = history().map { it.copy(services = setOf(ServiceType.BRAKE_PADS)) }
        assertThat(strategy.recommend(data, today).single().status).isEqualTo(RecommendationStatus.INSUFFICIENT_HISTORY)
    }
    @Test fun `strongly varying intervals require review`() {
        val data = listOf(visit("2026-01-01"), visit("2026-01-16"), visit("2026-07-01"))
        assertThat(strategy.recommend(data, today).single().status).isEqualTo(RecommendationStatus.INCONSISTENT_HISTORY)
    }
    @Test fun `vehicles are evaluated independently`() {
        val data = history() + visit("2026-06-01").copy(vehicleId = "car-2")
        assertThat(strategy.recommend(data, today).map { it.status })
            .containsExactly(RecommendationStatus.READY, RecommendationStatus.INSUFFICIENT_HISTORY)
    }
    @Test fun `empty history returns no recommendations`() {
        assertThat(strategy.recommend(emptyList(), today)).isEmpty()
    }
}
