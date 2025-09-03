package no.nav.melosys.skjema.integrasjon.altinn

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerRequest
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse
import no.nav.melosys.skjema.integrasjon.altinn.dto.Filter
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

private val log = KotlinLogging.logger { }

@Component
class ArbeidsgiverAltinnTilgangerConsumer(
    private val arbeidsgiverAltinnTilgangerClient: WebClient
) {

    fun hentTilganger(filter: Filter? = null): AltinnTilgangerResponse {
        log.info { "Kaller arbeidsgiver-altinn-tilganger med filter: $filter" }

        val request = AltinnTilgangerRequest(filter)

        val response = arbeidsgiverAltinnTilgangerClient.post()
            .uri("/altinn-tilganger")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(AltinnTilgangerResponse::class.java)
            .block()

        return response ?: throw RuntimeException("Fikk null response fra arbeidsgiver-altinn-tilganger")
    }
}