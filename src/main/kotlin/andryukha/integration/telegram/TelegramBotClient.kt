package andryukha.integration.telegram

import andryukha.integration.telegram.generated.model.GetMeResponse
import andryukha.integration.telegram.generated.model.SendMessageRequest
import andryukha.integration.telegram.generated.model.SendMessageResponse
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/** Bot tokens are part of Telegram URLs: never log request URLs or upstream exception text. */
class TelegramBotClient(private val properties: TelegramProperties, private val client: RestClient) : TelegramGateway {
    override fun getIdentity(): TelegramBotIdentity {
        ensureEnabled()
        val response = call {
            client.get().uri("/getMe").retrieve()
                .body(GetMeResponse::class.java)
        }
        val user = response.result.takeIf { response.ok } ?: throw TelegramUnavailableException()
        return TelegramBotIdentity(user.id, user.username)
    }

    override fun sendText(chatId: Long, text: String): TelegramMessageReceipt {
        ensureEnabled()
        val response = call {
            client.post().uri("/sendMessage").body(SendMessageRequest(chatId, text)).retrieve()
                .body(SendMessageResponse::class.java)
        }
        val message = response.result.takeIf { response.ok } ?: throw TelegramUnavailableException()
        return TelegramMessageReceipt(message.messageId)
    }

    private fun ensureEnabled() { if (!properties.enabled) throw TelegramDisabledException() }
    private fun <T : Any> call(action: () -> T?): T {
        val response = try { action() } catch (_: RestClientException) {
            // Deliberately drop the upstream cause: it can contain the bot token in its URL.
            throw TelegramUnavailableException()
        }
        return response ?: throw TelegramUnavailableException()
    }
}
