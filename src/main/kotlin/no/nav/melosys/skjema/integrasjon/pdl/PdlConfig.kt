package no.nav.melosys.skjema.integrasjon.pdl

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.felles.WebClientConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

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
        webClientBuilder: WebClient.Builder,
        pdlAuthFilter: PdlAuthFilterAzure,
        correlationIdOutgoingFilter: no.nav.melosys.skjema.integrasjon.felles.CorrelationIdOutgoingFilter
    ): WebClient {
        log.info { "Konfigurerer PDL-klient med base URL: $pdlUrl" }

        return webClientBuilder
            .baseUrl(pdlUrl)
            .defaultHeaders { headers ->
                headers.accept = listOf(MediaType.APPLICATION_JSON)
                headers.contentType = MediaType.APPLICATION_JSON
                headers.set(BEHANDLINGSNUMMER, MELOSYS_SKJEMA_BEHANDLINGSNUMMER)
            }
            .filter(pdlAuthFilter)
            .filter(correlationIdOutgoingFilter)
            .filter(WebClientConfig.errorFilter("Kall mot PDL feilet"))
            .build()
    }
}
