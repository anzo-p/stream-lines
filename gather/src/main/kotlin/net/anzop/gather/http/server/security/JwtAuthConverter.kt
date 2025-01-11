package net.anzop.gather.http.server.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class JwtAuthConverter : Converter<Jwt, JwtAuthenticationToken> {
    override fun convert(jwt: Jwt): JwtAuthenticationToken {
        val role = jwt.claims["role"] as String? ?: "USER"
        val authorities = listOf<GrantedAuthority>(
            SimpleGrantedAuthority("ROLE_$role")
        )
        return JwtAuthenticationToken(jwt, authorities)
    }
}
