package no.nav.melosys.skjema.integrasjon.altinn

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnFilter
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerRequest
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

private val log = KotlinLogging.logger { }

@Component
class ArbeidsgiverAltinnTilgangerConsumer(
    private val arbeidsgiverAltinnTilgangerClient: WebClient
) {

    fun hentTilganger(altinnFilter: AltinnFilter? = null): AltinnTilgangerResponse {
        log.info { "Kaller arbeidsgiver-altinn-tilganger med filter: $altinnFilter" }

        val request = AltinnTilgangerRequest(altinnFilter)

        val response = arbeidsgiverAltinnTilgangerClient.post()
            .uri("/altinn-tilganger")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(AltinnTilgangerResponse::class.java)
            .block()

        return response ?: throw RuntimeException("Fikk null response fra arbeidsgiver-altinn-tilganger")
    }
}