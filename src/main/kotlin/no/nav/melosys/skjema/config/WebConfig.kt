package no.nav.melosys.skjema.config

import no.nav.melosys.skjema.config.observability.CorrelationIdInterceptor
import no.nav.melosys.skjema.sikkerhet.AdminApiKeyInterceptor
import no.nav.melosys.skjema.sikkerhet.AdminTilgangInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Konfigurasjon for Spring Web MVC.
 * Registrerer interceptors for bl.a. MDC-håndtering og tilgang til admin-endepunktene.
 */
@Configuration
class WebConfig(
    private val correlationIdInterceptor: CorrelationIdInterceptor,
    private val adminApiKeyInterceptor: AdminApiKeyInterceptor,
    private val adminTilgangInterceptor: AdminTilgangInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(correlationIdInterceptor)
        // Nøkkelsjekken først, så kall som avvises i dag, avvises på samme måte
        registry.addInterceptor(adminApiKeyInterceptor).addPathPatterns("/admin/**")
        registry.addInterceptor(adminTilgangInterceptor).addPathPatterns("/admin/**")
    }
}
