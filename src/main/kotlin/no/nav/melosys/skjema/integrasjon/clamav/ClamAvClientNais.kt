package no.nav.melosys.skjema.integrasjon.clamav

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.exception.VedleggVirusFunnetException
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient

private val log = KotlinLogging.logger { }

@Service
@Profile("!local-q1 & !local-q2")
class ClamAvClientNais(
    private val clamAvWebClient: WebClient
) : ClamAvClient {

    override fun scan(fil: MultipartFile) {
        log.info { "Scanner fil '${fil.originalFilename}' for virus via ClamAV" }

        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("file1", object : ByteArrayResource(fil.bytes) {
            override fun getFilename(): String = fil.originalFilename ?: "file"
        }).contentType(MediaType.APPLICATION_OCTET_STREAM)

        val response = clamAvWebClient.put()
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .bodyToMono(Array<ClamAvScanResult>::class.java)
            .block()

        if (response.isNullOrEmpty()) {
            log.error { "ClamAV returnerte tomt svar for '${fil.originalFilename}'" }
            throw VedleggVirusFunnetException("Virusscanning feilet. Filen kan ikke lastes opp.")
        }

        response.forEach { result ->
            when (result.Result) {
                ClamAvStatus.FOUND -> {
                    log.warn { "Virus funnet i fil '${fil.originalFilename}': ${result.Filename}" }
                    throw VedleggVirusFunnetException("Filen inneholder virus og kan ikke lastes opp.")
                }
                ClamAvStatus.ERROR -> {
                    log.error { "ClamAV feilet ved scanning av '${fil.originalFilename}': ${result.Filename}" }
                    throw VedleggVirusFunnetException("Virusscanning feilet. Filen kan ikke lastes opp.")
                }
                ClamAvStatus.OK -> {}
            }
        }

        log.info { "Virusscanning fullført uten funn for '${fil.originalFilename}'" }
    }
}

private data class ClamAvScanResult(
    val Filename: String,
    val Result: ClamAvStatus
)

private enum class ClamAvStatus {
    FOUND, OK, ERROR
}
