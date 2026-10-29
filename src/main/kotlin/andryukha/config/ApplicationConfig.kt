package andryukha.config

import andryukha.domain.HistoryBasedRecommendationStrategy
import andryukha.domain.RecommendationStrategy
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

@ConfigurationProperties("andryukha.recommendations")
data class RecommendationProperties(
    val leadDays: Long = 14,
    val overdueReviewDays: Long = 60,
    val minimumIntervalDays: Long = 14,
    val maximumIntervalDays: Long = 730,
    val maximumIntervalRatio: Double = 3.0,
) {
    init {
        require(leadDays >= 0 && overdueReviewDays >= 0)
        require(minimumIntervalDays > 0 && maximumIntervalDays >= minimumIntervalDays)
        require(maximumIntervalRatio >= 1.0 && maximumIntervalRatio.isFinite())
    }
}

@ConfigurationProperties("andryukha")
data class ApplicationProperties(val timezone: String = "Europe/Moscow")

@Configuration
class ApplicationConfig {
    @Bean fun clock(properties: ApplicationProperties): Clock = Clock.system(ZoneId.of(properties.timezone))
    @Bean fun recommendationStrategy(properties: RecommendationProperties): RecommendationStrategy =
        HistoryBasedRecommendationStrategy(properties.leadDays, properties.overdueReviewDays,
            properties.minimumIntervalDays, properties.maximumIntervalDays, properties.maximumIntervalRatio)
}
