package net.anzop.gather.http.server.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class SharedSecretAuthFilter(
    private val expected: String
) : OncePerRequestFilter() {

    override fun doFilterInternal(req: HttpServletRequest, res: HttpServletResponse, chain: FilterChain) {
        val provided = req.getHeader("X-Api-Key")
        if (provided == null || !constantTimeEquals(provided, expected)) {
            res.status = 401
            return
        }

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken.authenticated("internal", null, emptyList())

        chain.doFilter(req, res)
    }

    override fun shouldNotFilter(req: HttpServletRequest): Boolean {
        val p = req.requestURI
        return p == "/" || p == "/health" || p.startsWith("/actuator/health")
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var r = 0
        for (i in a.indices) r = r or (a[i].code xor b[i].code)
        return r == 0
    }
}
