package no.nav.melosys.skjema.integrasjon.altinn

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.felles.AuthorizationHeaderInterceptorFactory
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

    companion object {
        private const val CLIENT_NAME = "arbeidsgiver-altinn-tilganger"
    }

    @Bean
    fun arbeidsgiverAltinnTilgangerRestClient(
        restClientBuilder: RestClient.Builder,
        authorizationHeaderInterceptorFactory: AuthorizationHeaderInterceptorFactory
    ): RestClient {
        log.info { "Konfigurerer ArbeidsgiverAltinnTilgangerClient med base URL: $arbeidsgiverAltinnTilgangerBaseUrl" }

        return restClientBuilder
            .baseUrl(arbeidsgiverAltinnTilgangerBaseUrl)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .requestInterceptor(authorizationHeaderInterceptorFactory.authorizationInterceptor(CLIENT_NAME))
            .build()
    }
}