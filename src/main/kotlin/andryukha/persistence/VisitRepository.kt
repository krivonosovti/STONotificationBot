package andryukha.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface VisitRepository : JpaRepository<VisitEntity, UUID> {
    fun findAllByExternalIdIn(externalIds: Collection<String>): List<VisitEntity>
    fun findAllByVisitedOnLessThanEqual(date: LocalDate): List<VisitEntity>
    fun findAllByCustomerId(customerId: String, pageable: Pageable): Page<VisitEntity>
}
