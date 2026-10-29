package andryukha.service

import andryukha.api.VisitPage
import andryukha.persistence.VisitRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VisitQueryService(private val repository: VisitRepository) {
    @Transactional(readOnly = true)
    fun list(customerId: String?, page: Int, size: Int): VisitPage {
        require(page >= 0 && size in 1..200) { "page >= 0; size от 1 до 200." }
        val pageable = PageRequest.of(page, size, Sort.by("visitedOn", "externalId"))
        val result = if (customerId == null) repository.findAll(pageable)
            else repository.findAllByCustomerId(customerId, pageable)
        return VisitPage(result.content.map { it.toDomain() }, page, size, result.totalElements)
    }
}
