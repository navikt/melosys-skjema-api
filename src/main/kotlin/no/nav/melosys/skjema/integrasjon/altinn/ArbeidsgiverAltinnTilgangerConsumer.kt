package no.nav.melosys.skjema.integrasjon.altinn

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnFilter
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerRequest
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

private val log = KotlinLogging.logger { }

@Component
class ArbeidsgiverAltinnTilgangerConsumer(
    private val arbeidsgiverAltinnTilgangerClient: RestClient
) {

    fun hentTilganger(altinnFilter: AltinnFilter? = null): AltinnTilgangerResponse {
        log.info { "Kaller arbeidsgiver-altinn-tilganger med filter: $altinnFilter" }

        val request = AltinnTilgangerRequest(altinnFilter)

        return arbeidsgiverAltinnTilgangerClient.post()
            .uri("/altinn-tilganger")
            .body(request)
            .retrieve()
            .body<AltinnTilgangerResponse>()
            ?: throw RuntimeException("Fikk null response fra arbeidsgiver-altinn-tilganger")
    }
}
