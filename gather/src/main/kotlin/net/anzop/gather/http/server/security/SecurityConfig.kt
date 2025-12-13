package net.anzop.gather.http.server.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig(
    @param:Value("\${internal.shared-secret}") private val sharedSecret: String
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .addFilterBefore(SharedSecretAuthFilter(sharedSecret),
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter::class.java
            )
            .authorizeHttpRequests {
                it
                    .requestMatchers("/", "/health", "/actuator/health").permitAll()
                    .anyRequest().authenticated()
            }
            .build()
}
