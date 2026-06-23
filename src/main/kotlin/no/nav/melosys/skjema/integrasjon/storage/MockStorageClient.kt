package no.nav.melosys.skjema.integrasjon.storage

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

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

    private val restClient: RestClient = RestClient.builder().baseUrl(baseUrl).build()

    override fun lastOpp(storageReferanse: String, data: ByteArray, contentType: String) {
        log.info { "Mock-storage: laster opp '$storageReferanse' (${data.size} bytes)" }
        restClient.put()
            .uri { it.queryParam("ref", storageReferanse).build() }
            .contentType(MediaType.parseMediaType(contentType))
            .body(data)
            .retrieve()
            .toBodilessEntity()
    }

    override fun slett(storageReferanse: String) {
        log.info { "Mock-storage: sletter '$storageReferanse'" }
        restClient.delete()
            .uri { it.queryParam("ref", storageReferanse).build() }
            .retrieve()
            .toBodilessEntity()
    }

    override fun hent(storageReferanse: String): ByteArray {
        log.info { "Mock-storage: henter '$storageReferanse'" }
        return restClient.get()
            .uri { it.queryParam("ref", storageReferanse).build() }
            .retrieve()
            .body(ByteArray::class.java) ?: error("Vedlegg '$storageReferanse' ikke funnet i mock-storage")
    }
}
