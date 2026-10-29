package andryukha.api

import andryukha.service.TelegramService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.*

data class TelegramTestMessageRequest(val chatId: Long, @field:NotBlank @field:Size(max = 4096) val text: String)

@RestController
@RequestMapping("/api/v1/integrations/telegram")
class TelegramController(private val service: TelegramService) {
    @GetMapping("/status")
    fun status() = service.status()

    @PostMapping("/test-message")
    fun send(@Valid @RequestBody request: TelegramTestMessageRequest) = service.sendTestMessage(request.chatId, request.text)
}
