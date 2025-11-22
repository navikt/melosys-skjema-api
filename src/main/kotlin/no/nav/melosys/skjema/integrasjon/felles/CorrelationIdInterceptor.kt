package no.nav.melosys.skjema.integrasjon.felles

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.melosys.skjema.integrasjon.felles.MDCOperations.Companion.CORRELATION_ID
import no.nav.melosys.skjema.integrasjon.felles.MDCOperations.Companion.MDC_CALL_ID
import no.nav.melosys.skjema.integrasjon.felles.MDCOperations.Companion.MDC_CONSUMER_ID
import no.nav.melosys.skjema.integrasjon.felles.MDCOperations.Companion.MDC_USER_ID
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Interceptor som populerer MDC med correlation ID og andre metadata ved innkommende requests.
 * Rydder opp i MDC etter at request er ferdig behandlet.
 */
@Component
class CorrelationIdInterceptor(
    private val tokenValidationContextHolder: TokenValidationContextHolder
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val callId = MDCOperations.generateCallId()
        val userId = getUserIdFromToken()

        MDCOperations.putToMDC(MDC_CALL_ID, callId)
        MDCOperations.putToMDC(MDC_USER_ID, userId ?: "")
        MDCOperations.putToMDC(CORRELATION_ID, MDCOperations.getCorrelationId(request))
        MDCOperations.putToMDC(MDC_CONSUMER_ID, MDCOperations.getSystembruker())

        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        // Rydd opp MDC etter request
        MDCOperations.remove(MDC_CALL_ID)
        MDCOperations.remove(MDC_USER_ID)
        MDCOperations.remove(MDC_CONSUMER_ID)
        MDCOperations.remove(CORRELATION_ID)
    }

    private fun getUserIdFromToken(): String? {
        val context = tokenValidationContextHolder.getTokenValidationContext()
        return context?.firstValidToken?.jwtTokenClaims?.getStringClaim("pid")
    }
}
