package no.nav.melosys.skjema.integrasjon.altinn

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.felles.OAuth2AuthorizationHeaderProvider
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnFilter
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerRequest
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

private val log = KotlinLogging.logger { }

@Component
class ArbeidsgiverAltinnTilgangerConsumer(
    private val arbeidsgiverAltinnTilgangerClient: RestClient,
    private val authorizationHeaderProvider: OAuth2AuthorizationHeaderProvider
) {

    companion object {
        private const val CLIENT_NAME = "arbeidsgiver-altinn-tilganger"
    }

    fun hentTilganger(altinnFilter: AltinnFilter? = null): AltinnTilgangerResponse {
        log.info { "Kaller arbeidsgiver-altinn-tilganger med filter: $altinnFilter" }

        val request = AltinnTilgangerRequest(altinnFilter)

        return arbeidsgiverAltinnTilgangerClient.post()
            .uri("/altinn-tilganger")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeaderProvider.authorizationHeader(CLIENT_NAME))
            .body(request)
            .retrieve()
            .body<AltinnTilgangerResponse>()
            ?: throw RuntimeException("Fikk null response fra arbeidsgiver-altinn-tilganger")
    }
}