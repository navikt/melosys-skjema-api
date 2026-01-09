package no.nav.melosys.skjema.config

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.filter.RequestContextFilter

@Configuration
class AsyncRequestContextConfiguration {

    /**
     * RequestContextListener er allerede registrert av JWT library
     * Vi trenger bare RequestContextFilter med høy prioritet
     *
     * Alternativt kan vi bruke RequestContextFilter med høy prioritet
     * for å sikre at request context er tilgjengelig i alle tråder
     */
    @Bean
    fun requestContextFilter(): FilterRegistrationBean<RequestContextFilter> {
        val registration = FilterRegistrationBean(RequestContextFilter())
        registration.order = Ordered.HIGHEST_PRECEDENCE
        registration.setName("requestContextFilter")
        return registration
    }
}