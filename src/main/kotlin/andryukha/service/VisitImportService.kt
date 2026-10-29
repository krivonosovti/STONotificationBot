package andryukha.service

import andryukha.api.ImportResult
import andryukha.domain.Visit
import andryukha.persistence.VisitEntity
import andryukha.persistence.VisitRepository
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Clock
import java.time.LocalDate

@Service
class VisitImportService(
    private val repository: VisitRepository,
    private val clock: Clock,
    private val metrics: MeterRegistry,
) {
    @Transactional
    fun import(visits: List<Visit>): ImportResult {
        require(visits.map { it.externalId }.distinct().size == visits.size) {
            "В одном пакете externalId должны быть уникальны."
        }
        require(visits.none { it.visitedOn.isAfter(LocalDate.now(clock)) }) {
            "Дата фактического визита не может быть в будущем."
        }
        val existing = repository.findAllByExternalIdIn(visits.map { it.externalId }).associateBy { it.externalId }
        var inserted = 0
        var updated = 0
        var unchanged = 0
        visits.forEach { visit ->
            val entity = existing[visit.externalId]
            when {
                entity == null -> { repository.save(VisitEntity.from(visit, clock.instant())); inserted++ }
                entity.toDomain() == visit -> unchanged++
                else -> { entity.updateFrom(visit, clock.instant()); updated++ }
            }
        }
        repository.flush()
        val result = ImportResult(inserted, updated, unchanged)
        // Count committed work only. Failed or rolled-back imports must not inflate metrics.
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                mapOf("inserted" to inserted, "updated" to updated, "unchanged" to unchanged).forEach { (outcome, count) ->
                    metrics.counter("andryukha.import.visits", "outcome", outcome).increment(count.toDouble())
                }
            }
        })
        return result
    }
}
