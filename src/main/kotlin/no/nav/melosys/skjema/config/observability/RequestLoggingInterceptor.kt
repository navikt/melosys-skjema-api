package no.nav.melosys.skjema.config.observability

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

private val log = KotlinLogging.logger {}

/**
 * Logger request method/URL og response status for innkommende requests.
 */
@Component
class RequestLoggingInterceptor : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
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
    }
}
