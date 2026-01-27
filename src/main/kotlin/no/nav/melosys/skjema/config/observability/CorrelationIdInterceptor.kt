package no.nav.melosys.skjema.config.observability

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.melosys.skjema.config.observability.MDCOperations.Companion.CORRELATION_ID
import no.nav.melosys.skjema.config.observability.MDCOperations.Companion.MDC_CALL_ID
import no.nav.melosys.skjema.config.observability.MDCOperations.Companion.MDC_CONSUMER_ID
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

private val log = KotlinLogging.logger {}

/**
 * Interceptor som populerer MDC med correlation ID og andre metadata ved innkommende requests.
 * Logger også request method/URL og response status.
 * Rydder opp i MDC etter at request er ferdig behandlet.
 */
@Component
class CorrelationIdInterceptor : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val callId = MDCOperations.generateCallId()
        val correlationId = MDCOperations.getCorrelationId(request)

        MDCOperations.putToMDC(MDC_CALL_ID, callId)
        MDCOperations.putToMDC(CORRELATION_ID, correlationId)
        MDCOperations.putToMDC(MDC_CONSUMER_ID, MDCOperations.getSystembruker())

        response.setHeader(MDCOperations.X_CORRELATION_ID, correlationId)

        log.info { "Incoming request: ${request.method} ${request.requestURI}" }

        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        log.info { "Response: ${request.method} ${request.requestURI} - ${response.status}" }

        // Rydd opp MDC etter request
        MDCOperations.remove(MDC_CALL_ID)
        MDCOperations.remove(MDC_CONSUMER_ID)
        MDCOperations.remove(CORRELATION_ID)
    }
}
