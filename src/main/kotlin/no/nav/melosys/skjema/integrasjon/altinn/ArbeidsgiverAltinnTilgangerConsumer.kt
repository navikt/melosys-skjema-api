package no.nav.melosys.skjema.integrasjon.altinn

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.felles.OAuth2AuthorizationHeaderProvider
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnFilter
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerRequest
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

private val log = KotlinLogging.logger { }

@Component
class ArbeidsgiverAltinnTilgangerConsumer(
    private val arbeidsgiverAltinnTilgangerClient: WebClient,
    private val authorizationHeaderProvider: OAuth2AuthorizationHeaderProvider
) {

    companion object {
        private const val CLIENT_NAME = "arbeidsgiver-altinn-tilganger"
    }

    fun hentTilganger(altinnFilter: AltinnFilter? = null): AltinnTilgangerResponse {
        log.info { "Kaller arbeidsgiver-altinn-tilganger med filter: $altinnFilter" }

        val request = AltinnTilgangerRequest(altinnFilter)

        val response = arbeidsgiverAltinnTilgangerClient.post()
            .uri("/altinn-tilganger")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeaderProvider.authorizationHeader(CLIENT_NAME))
            .bodyValue(request)
            .retrieve()
            .bodyToMono(AltinnTilgangerResponse::class.java)
            .block()

        return response ?: throw RuntimeException("Fikk null response fra arbeidsgiver-altinn-tilganger")
    }
}