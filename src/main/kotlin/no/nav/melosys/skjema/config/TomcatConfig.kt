package no.nav.melosys.skjema.config

import org.springframework.boot.tomcat.TomcatConnectorCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TomcatConfig {

    /**
     * Tomcat 11.0.27 avviser requester med flere enn 10 språk i Accept-Language (maxLocaleCount) med 500,
     * før Spring får håndtert dem. Frontend-proxyen videresender brukerens nettleserheader uendret.
     *
     * TODO: Fjern denne klassen når Spring Boot 4.1.3 er released og vi har oppgradert. Den tar inn en
     *  Tomcat-versjon der overskytende språk ignoreres (apache/tomcat@c639c7e), og støtter i tillegg
     *  `server.tomcat.max-locale-count`.
     */
    @Bean
    fun maxLocaleCountCustomizer() = TomcatConnectorCustomizer { connector ->
        connector.maxLocaleCount = MAX_LOCALE_COUNT
    }

    companion object {
        const val MAX_LOCALE_COUNT = 100
    }
}
