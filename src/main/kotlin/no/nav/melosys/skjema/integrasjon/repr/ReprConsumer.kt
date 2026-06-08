package no.nav.melosys.skjema.integrasjon.repr

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.felles.OAuth2AuthorizationHeaderProvider
import no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux

private val log = KotlinLogging.logger { }

@Component
class ReprConsumer(
    private val reprClientTokenX: WebClient,
    private val authorizationHeaderProvider: OAuth2AuthorizationHeaderProvider
) {

    companion object {
        private const val CLIENT_NAME = "repr-api"
    }

    @Cacheable(value = ["fullmakter"], key = "@cacheKeyProvider.getUserId()", condition = "@cacheKeyProvider.getUserId() != null")
    fun hentKanRepresentere(): List<Fullmakt> {
        log.info { "Kaller repr-api /api/v2/eksternbruker/fullmakt/kan-representere" }

        val response = reprClientTokenX.get()
            .uri("/api/v2/eksternbruker/fullmakt/kan-representere")
            .header(HttpHeaders.AUTHORIZATION, authorizationHeaderProvider.authorizationHeader(CLIENT_NAME))
            .retrieve()
            .bodyToFlux<Fullmakt>()
            .collectList()
            .block()

        return response ?: emptyList()
    }
}
