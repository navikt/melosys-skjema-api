package no.nav.melosys.skjema.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.melosys.skjema.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.web.servlet.HandlerInterceptor

class RestControllerInterceptor : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        // Skip interceptor for error pages to avoid ThreadLocal conflicts
        if (request.requestURI == "/error") {
            return true
        }
        
        ThreadLocalAccessInfo.beforeControllerRequest(
            request.requestURI,
        )
        super.preHandle(request, response, handler)
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        // Skip interceptor for error pages to avoid ThreadLocal conflicts
        if (request.requestURI == "/error") {
            return
        }
        
        ThreadLocalAccessInfo.afterControllerRequest(request.requestURI)
    }
}

