package andryukha.domain

import java.time.LocalDate

enum class ServiceType { MAINTENANCE, OIL_CHANGE, BRAKE_PADS, OTHER }
enum class VisitStatus { COMPLETED, CANCELLED }

data class Visit(
    val externalId: String,
    val customerId: String,
    val vehicleId: String,
    val visitedOn: LocalDate,
    val odometerKm: Int?,
    val services: Set<ServiceType>,
    val status: VisitStatus,
)
