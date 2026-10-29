package andryukha.integration.telegram

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@ConfigurationProperties("andryukha.telegram")
class TelegramProperties(val enabled: Boolean = false, val botToken: String = "") {
    init {
        require(!enabled || botToken.matches(Regex("[0-9]+:[A-Za-z0-9_-]+"))) {
            "При включённом Telegram задайте TELEGRAM_BOT_TOKEN в формате BotFather."
        }
    }
}

@Configuration
class TelegramConfig {
    @Bean fun telegramGateway(properties: TelegramProperties): TelegramGateway {
        val factory = JdkClientHttpRequestFactory(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build())
        factory.setReadTimeout(Duration.ofSeconds(5))
        // Isolated client avoids recording a token-bearing URL in HTTP observation tags.
        val client = RestClient.builder().requestFactory(factory)
            .baseUrl("https://api.telegram.org/bot${properties.botToken}").build()
        return TelegramBotClient(properties, client)
    }
}
