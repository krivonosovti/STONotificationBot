package andryukha.service

import andryukha.integration.telegram.TelegramGateway
import andryukha.integration.telegram.TelegramMessageReceipt
import andryukha.integration.telegram.TelegramProperties
import org.springframework.stereotype.Service

data class TelegramConnectionStatus(val enabled: Boolean, val connected: Boolean, val botId: Long? = null, val username: String? = null)

@Service
class TelegramService(private val gateway: TelegramGateway, private val properties: TelegramProperties) {
    fun status(): TelegramConnectionStatus {
        if (!properties.enabled) return TelegramConnectionStatus(false, false)
        val identity = gateway.getIdentity()
        return TelegramConnectionStatus(true, true, identity.id, identity.username)
    }
    fun sendTestMessage(chatId: Long, text: String): TelegramMessageReceipt = gateway.sendText(chatId, text)
}
