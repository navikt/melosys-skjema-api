package no.nav.melosys.skjema.integrasjon.pdl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

private val log = KotlinLogging.logger { }

@Configuration
class PdlConfig(
    @param:Value("\${pdl.url}") private val pdlUrl: String
) {

    companion object {
        private const val BEHANDLINGSNUMMER = "behandlingsnummer"
        private const val MELOSYS_SKJEMA_BEHANDLINGSNUMMER = "B272"
    }

    @Bean
    fun pdlClient(
        restClientBuilder: RestClient.Builder,
        correlationIdOutgoingInterceptor: no.nav.melosys.skjema.config.observability.CorrelationIdOutgoingInterceptor
    ): RestClient {
        log.info { "Konfigurerer PDL-klient med base URL: $pdlUrl" }

        return restClientBuilder
            .baseUrl(pdlUrl)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(BEHANDLINGSNUMMER, MELOSYS_SKJEMA_BEHANDLINGSNUMMER)
            .requestInterceptor(correlationIdOutgoingInterceptor)
            .build()
    }
}
