package no.nav.melosys.skjema.config

import no.nav.melosys.skjema.config.observability.CorrelationIdInterceptor
import no.nav.melosys.skjema.sikkerhet.AdminApiKeyInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Konfigurasjon for Spring Web MVC.
 * Registrerer interceptors for bl.a. MDC-håndtering og API-nøkkel på admin-endepunktene.
 */
@Configuration
class WebConfig(
    private val correlationIdInterceptor: CorrelationIdInterceptor,
    private val adminApiKeyInterceptor: AdminApiKeyInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(correlationIdInterceptor)
        registry.addInterceptor(adminApiKeyInterceptor).addPathPatterns("/admin/**")
    }
}
