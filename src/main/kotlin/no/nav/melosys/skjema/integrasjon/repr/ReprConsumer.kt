package no.nav.melosys.skjema.integrasjon.repr

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

private val log = KotlinLogging.logger { }

@Component
class ReprConsumer(
    private val reprClientTokenX: WebClient
) {

    @Cacheable(value = ["fullmakter"], key = "@reprService.getBrukerPid()")
    fun hentKanRepresentere(): List<Fullmakt> {
        log.info { "Kaller repr-api /api/v2/eksternbruker/fullmakt/kan-representere" }

        val response = reprClientTokenX.get()
            .uri("/api/v2/eksternbruker/fullmakt/kan-representere")
            .retrieve()
            .bodyToFlux(Fullmakt::class.java)
            .collectList()
            .block()

        return response ?: emptyList()
    }
}
