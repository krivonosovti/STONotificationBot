package andryukha

import andryukha.integration.telegram.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.*
import org.springframework.web.client.RestClient

class TelegramBotClientTest {
    private val token = "123456:test-token"
    private val builder = RestClient.builder().baseUrl("https://api.telegram.org/bot$token")
    private val server = MockRestServiceServer.bindTo(builder).build()
    private val client = TelegramBotClient(TelegramProperties(true, token), builder.build())

    @Test fun `getMe reads the bot identity and tolerates extra Telegram fields`() {
        server.expect(requestTo("https://api.telegram.org/bot$token/getMe"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""{"ok":true,"result":{"id":123456,"is_bot":true,"first_name":"Андрюха","username":"andryukha_bot"}}""", MediaType.APPLICATION_JSON))
        assertThat(client.getIdentity()).isEqualTo(TelegramBotIdentity(123456, "andryukha_bot"))
        server.verify()
    }
    @Test fun `sendMessage uses Telegram field names and returns a receipt`() {
        server.expect(requestTo("https://api.telegram.org/bot$token/sendMessage"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""{"chat_id":987654,"text":"Привет"}"""))
            .andRespond(withSuccess("""{"ok":true,"result":{"message_id":42,"date":1,"chat":{"id":987654,"type":"private"}}}""", MediaType.APPLICATION_JSON))
        assertThat(client.sendText(987654, "Привет").messageId).isEqualTo(42)
        server.verify()
    }
    @Test fun `disabled integration does not make HTTP requests`() {
        val disabled = TelegramBotClient(TelegramProperties(), builder.build())
        assertThatThrownBy { disabled.getIdentity() }.isInstanceOf(TelegramDisabledException::class.java)
        server.verify()
    }
    @Test fun `upstream error does not expose the token or cause`() {
        server.expect(requestTo("https://api.telegram.org/bot$token/getMe"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("bad token $token"))
        assertThatThrownBy { client.getIdentity() }.isInstanceOf(TelegramUnavailableException::class.java)
            .hasNoCause().hasMessageNotContaining(token)
        server.verify()
    }
    @Test fun `unsuccessful Telegram envelope is rejected`() {
        server.expect(requestTo("https://api.telegram.org/bot$token/getMe"))
            .andRespond(withSuccess("""{"ok":false,"description":"bad token"}""", MediaType.APPLICATION_JSON))
        assertThatThrownBy { client.getIdentity() }.isInstanceOf(TelegramUnavailableException::class.java)
    }
    @Test fun `enabled integration requires a token`() {
        assertThatThrownBy { TelegramProperties(true, "") }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
