package no.nav.melosys.skjema.integrasjon.repr

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.repr.dto.KanRepresentereResponse
import no.nav.melosys.skjema.integrasjon.repr.dto.KanRepresenteresAvResponse
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

private val log = KotlinLogging.logger { }

@Component
class ReprConsumer(
    private val reprClientTokenX: WebClient
) {

    fun hentKanRepresentere(): KanRepresentereResponse {
        log.info { "Kaller repr-api /eksternbruker/fullmakt/kan-representere" }

        val response = reprClientTokenX.get()
            .uri("/eksternbruker/fullmakt/kan-representere")
            .retrieve()
            .bodyToMono(KanRepresentereResponse::class.java)
            .block()

        return response ?: throw RuntimeException("Fikk null response fra repr-api")
    }

    fun hentKanRepresenteresAvForInnloggetBruker(): KanRepresenteresAvResponse {
        log.info { "Kaller repr-api /eksternbruker/kan-representeres-av" }

        val response = reprClientTokenX.get()
            .uri("/eksternbruker/kan-representeres-av")
            .retrieve()
            .bodyToMono(KanRepresenteresAvResponse::class.java)
            .block()

        return response ?: throw RuntimeException("Fikk null response fra repr-api")
    }
}
