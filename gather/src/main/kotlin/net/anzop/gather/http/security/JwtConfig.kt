package net.anzop.gather.http.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import javax.crypto.spec.SecretKeySpec

@Configuration
class JwtConfig(
    private val jwtProperties: JwtProperties
) {
    @Configuration
    @ConfigurationProperties(prefix = "jwt")
    class JwtProperties {
        lateinit var issuer: String
        lateinit var audience: String
        lateinit var secretKey: String
    }

    @Bean
    fun jwtDecoder(): JwtDecoder =
        SecretKeySpec(jwtProperties.secretKey.toByteArray(), "HmacSHA256")
            .let { NimbusJwtDecoder.withSecretKey(it).build() }
}
