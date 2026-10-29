package andryukha.api

import andryukha.domain.ServiceType
import andryukha.domain.Visit
import andryukha.domain.VisitStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.time.LocalDate

data class VisitImportRequest(
    @field:Valid @field:Size(min = 1, max = 1000)
    val visits: List<VisitRequest>,
)

data class VisitRequest(
    @field:NotBlank @field:Size(max = 100) val externalId: String,
    @field:NotBlank @field:Size(max = 100) val customerId: String,
    @field:NotBlank @field:Size(max = 100) val vehicleId: String,
    val visitedOn: LocalDate,
    @field:Min(0) @field:Max(10_000_000) val odometerKm: Int? = null,
    @field:NotEmpty val services: Set<ServiceType>,
    val status: VisitStatus = VisitStatus.COMPLETED,
) {
    fun toDomain() = Visit(externalId.trim(), customerId.trim(), vehicleId.trim(), visitedOn, odometerKm, services, status)
}

data class ImportResult(val inserted: Int, val updated: Int, val unchanged: Int)
data class VisitPage(val items: List<Visit>, val page: Int, val size: Int, val totalElements: Long)
