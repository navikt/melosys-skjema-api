package no.nav.melosys.skjema.integrasjon.altinn

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

private val log = KotlinLogging.logger { }

@Configuration
class ArbeidsgiverAltinnTilgangerClientProducer(
    @param:Value("\${arbeidsgiver.altinn.tilganger.url}") private val arbeidsgiverAltinnTilgangerBaseUrl: String,
) {

    @Bean
    fun arbeidsgiverAltinnTilgangerClient(
        restClientBuilder: RestClient.Builder
    ): RestClient {
        log.info { "Konfigurerer ArbeidsgiverAltinnTilgangerConsumer med base URL: $arbeidsgiverAltinnTilgangerBaseUrl" }

        return restClientBuilder
            .baseUrl(arbeidsgiverAltinnTilgangerBaseUrl)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }
}