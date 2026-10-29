package andryukha.integration.telegram

data class TelegramBotIdentity(val id: Long, val username: String?)
data class TelegramMessageReceipt(val messageId: Long)

interface TelegramGateway {
    fun getIdentity(): TelegramBotIdentity
    fun sendText(chatId: Long, text: String): TelegramMessageReceipt
}

class TelegramDisabledException : RuntimeException("Telegram отключён. Укажите токен и включите интеграцию.")
class TelegramUnavailableException : RuntimeException("Telegram не подтвердил запрос. Проверьте токен, chat_id и доступность API.")
