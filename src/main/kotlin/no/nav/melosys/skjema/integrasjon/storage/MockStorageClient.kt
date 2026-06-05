package no.nav.melosys.skjema.integrasjon.storage

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

private val log = KotlinLogging.logger { }

/**
 * Storage-klient som lagrer vedlegg via melosys-mock (lokal utvikling).
 * Brukes i `local`-profil — sender ingen trafikk ut av PC-en.
 */
@Service
@Profile("local")
class MockStorageClient(
    @param:Value("\${vedlegg.mock-storage-url}") private val baseUrl: String
) : VedleggStorageClient {

    private companion object {
        const val MAX_IN_MEMORY_SIZE_BYTES = 16 * 1024 * 1024
    }

    private val webClient: WebClient = WebClient.builder()
        .baseUrl(baseUrl)
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs { it.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE_BYTES) }
                .build()
        )
        .build()

    override fun lastOpp(storageReferanse: String, data: ByteArray, contentType: String) {
        log.info { "Mock-storage: laster opp '$storageReferanse' (${data.size} bytes)" }
        webClient.put()
            .uri { it.queryParam("ref", storageReferanse).build() }
            .contentType(MediaType.parseMediaType(contentType))
            .bodyValue(data)
            .retrieve()
            .toBodilessEntity()
            .block()
    }

    override fun slett(storageReferanse: String) {
        log.info { "Mock-storage: sletter '$storageReferanse'" }
        webClient.delete()
            .uri { it.queryParam("ref", storageReferanse).build() }
            .retrieve()
            .toBodilessEntity()
            .block()
    }

    override fun hent(storageReferanse: String): ByteArray {
        log.info { "Mock-storage: henter '$storageReferanse'" }
        return webClient.get()
            .uri { it.queryParam("ref", storageReferanse).build() }
            .retrieve()
            .bodyToMono(ByteArray::class.java)
            .block() ?: error("Vedlegg '$storageReferanse' ikke funnet i mock-storage")
    }
}
