package no.nav.melosys.skjema.config

import no.nav.melosys.skjema.integrasjon.felles.CorrelationIdInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Konfigurasjon for Spring Web MVC.
 * Registrerer interceptors for bl.a. MDC-håndtering.
 */
@Configuration
class WebConfig(
    private val correlationIdInterceptor: CorrelationIdInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(correlationIdInterceptor)
    }
}
