package no.nav.melosys.skjema.service.pdf

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import no.nav.melosys.skjema.dto.InnsendtSkjemaResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

/**
 * Service for å generere PDF fra innsendt skjema.
 *
 * Bruker skjemadefinisjon for å hente korrekte labels/tekster
 * slik at PDF-en viser samme tekster som ble vist til bruker ved innsending.
 */
@Service
class PdfGeneratorService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val htmlBuilder = HtmlDokumentBuilder()

    /**
     * Genererer PDF fra innsendt skjema.
     *
     * @param innsendtSkjema Innsendt skjema med data og definisjon
     * @return PDF som byte array
     */
    fun genererPdf(innsendtSkjema: InnsendtSkjemaResponse): ByteArray {
        logger.info("Genererer PDF for skjema ${innsendtSkjema.skjemaId}")

        val html = htmlBuilder.byggHtml(innsendtSkjema)

        // Debug: Skriv HTML til fil for inspeksjon
        skrivDebugHtml(innsendtSkjema.referanseId, html)

        return konverterTilPdf(html)
    }

    private fun skrivDebugHtml(referanseId: String, html: String) {
        try {
            java.io.File("build/test-output/debug-$referanseId.html").apply {
                parentFile?.mkdirs()
                writeText(html)
            }
        } catch (e: Exception) {
            logger.debug("Kunne ikke skrive debug-HTML: ${e.message}")
        }
    }

    /**
     * Konverterer HTML til PDF ved hjelp av OpenHTMLToPDF.
     */
    private fun konverterTilPdf(html: String): ByteArray {
        val outputStream = ByteArrayOutputStream()

        PdfRendererBuilder()
            .withHtmlContent(html, null)
            .toStream(outputStream)
            .run()

        return outputStream.toByteArray()
    }
}
