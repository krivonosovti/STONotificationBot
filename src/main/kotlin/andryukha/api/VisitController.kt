package andryukha.api

import andryukha.service.VisitQueryService
import andryukha.service.VisitImportService
import andryukha.service.RecommendationService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1")
class VisitController(
    private val importService: VisitImportService,
    private val recommendationService: RecommendationService,
    private val queryService: VisitQueryService,
) {
    @PostMapping("/imports/visits")
    fun import(@Valid @RequestBody request: VisitImportRequest): ImportResult =
        importService.import(request.visits.map { it.toDomain() })

    @GetMapping("/visits")
    fun list(@RequestParam(required = false) customerId: String?,
             @RequestParam(defaultValue = "0") page: Int,
             @RequestParam(defaultValue = "50") size: Int): VisitPage {
        return queryService.list(customerId, page, size)
    }

    @GetMapping("/recommendations")
    fun recommendations(@RequestParam(required = false) asOf: LocalDate?) = recommendationService.preview(asOf)
}
