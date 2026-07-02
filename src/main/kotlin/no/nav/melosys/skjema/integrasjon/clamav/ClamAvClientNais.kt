package no.nav.melosys.skjema.integrasjon.clamav

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.exception.VedleggVirusFunnetException
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.multipart.MultipartFile

private val log = KotlinLogging.logger { }

@Service
@Profile("!local-q1 & !local-q2")
class ClamAvClientNais(
    private val clamAvRestClient: RestClient
) : ClamAvClient {

    override fun scan(fil: MultipartFile) {
        log.info { "Scanner fil '${fil.originalFilename}' for virus via ClamAV" }

        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("file1", object : ByteArrayResource(fil.bytes) {
            override fun getFilename(): String = fil.originalFilename ?: "file"
        }).contentType(MediaType.APPLICATION_OCTET_STREAM)

        val response = clamAvRestClient.put()
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(bodyBuilder.build())
            .retrieve()
            .body<Array<ClamAvScanResult>>()

        if (response.isNullOrEmpty()) {
            log.error { "ClamAV returnerte tomt svar for '${fil.originalFilename}'" }
            throw VedleggVirusFunnetException("Virusscanning feilet. Filen kan ikke lastes opp.")
        }

        response.forEach { scanResult ->
            when (scanResult.result) {
                ClamAvStatus.FOUND -> {
                    log.warn { "Virus funnet i fil '${fil.originalFilename}': ${scanResult.filename}" }
                    throw VedleggVirusFunnetException("Filen inneholder virus og kan ikke lastes opp.")
                }
                ClamAvStatus.ERROR -> {
                    log.error { "ClamAV feilet ved scanning av '${fil.originalFilename}': ${scanResult.filename}" }
                    throw VedleggVirusFunnetException("Virusscanning feilet. Filen kan ikke lastes opp.")
                }
                ClamAvStatus.OK -> {}
            }
        }

        log.info { "Virusscanning fullført uten funn for '${fil.originalFilename}'" }
    }
}

private data class ClamAvScanResult(
    val filename: String = "",
    val result: ClamAvStatus = ClamAvStatus.OK
)

private enum class ClamAvStatus {
    @com.fasterxml.jackson.annotation.JsonEnumDefaultValue
    OK, FOUND, ERROR
}
