package andryukha.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        // Stateless machine API. No cookies, browser forms or session authentication.
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            // Management runs on a separate Docker-only port; never publish it externally.
            it.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus").permitAll()
                .anyRequest().authenticated()
        }
        .httpBasic(Customizer.withDefaults())
        .build()
}
