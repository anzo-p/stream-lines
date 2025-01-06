package net.anzop.gather.http.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {
    private val securedEndpoints = listOf(
        "/api/admin/maintenance/fetch",
        "/api/admin/maintenance/redo-index",
        "/api/admin/maintenance/bar-data"
    )

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .authorizeHttpRequests { auth ->
                securedEndpoints.forEach { auth.requestMatchers(it).hasAuthority("ROLE_ADMIN") }
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(JwtAuthConverter())
                }
            }
            .build()
}
