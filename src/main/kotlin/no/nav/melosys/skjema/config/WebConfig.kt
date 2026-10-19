package no.nav.melosys.skjema.config

import no.nav.melosys.skjema.config.observability.RequestLoggingInterceptor
import no.nav.melosys.skjema.sikkerhet.AdminTilgangInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Konfigurasjon for Spring Web MVC.
 * Registrerer interceptors for bl.a. request-logging og driftsgruppen på admin-endepunktene.
 */
@Configuration
class WebConfig(
    private val requestLoggingInterceptor: RequestLoggingInterceptor,
    private val adminTilgangInterceptor: AdminTilgangInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(requestLoggingInterceptor)
        registry.addInterceptor(adminTilgangInterceptor).addPathPatterns("/admin/**")
    }
}
