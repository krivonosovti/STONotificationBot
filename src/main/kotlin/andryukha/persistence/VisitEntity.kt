package andryukha.persistence

import andryukha.domain.ServiceType
import andryukha.domain.Visit
import andryukha.domain.VisitStatus
import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "visits")
class VisitEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "external_id", nullable = false, unique = true, length = 100)
    val externalId: String,
    @Column(name = "customer_id", nullable = false, length = 100)
    var customerId: String,
    @Column(name = "vehicle_id", nullable = false, length = 100)
    var vehicleId: String,
    @Column(name = "visited_on", nullable = false)
    var visitedOn: LocalDate,
    @Column(name = "odometer_km")
    var odometerKm: Int?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: VisitStatus,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "visit_services", joinColumns = [JoinColumn(name = "visit_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 40)
    var services: MutableSet<ServiceType> = mutableSetOf(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Version var version: Long? = null,
) {
    fun toDomain() = Visit(externalId, customerId, vehicleId, visitedOn, odometerKm, services.toSet(), status)

    fun updateFrom(visit: Visit, now: Instant) {
        customerId = visit.customerId
        vehicleId = visit.vehicleId
        visitedOn = visit.visitedOn
        odometerKm = visit.odometerKm
        status = visit.status
        services.clear()
        services.addAll(visit.services)
        updatedAt = now
    }

    companion object {
        fun from(visit: Visit, now: Instant) = VisitEntity(externalId = visit.externalId,
            customerId = visit.customerId, vehicleId = visit.vehicleId, visitedOn = visit.visitedOn,
            odometerKm = visit.odometerKm, status = visit.status, services = visit.services.toMutableSet(), updatedAt = now)
    }
}
