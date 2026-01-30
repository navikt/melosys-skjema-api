package no.nav.melosys.skjema.service.pdf

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import no.nav.melosys.skjema.dto.InnsendtSkjemaResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Service for å generere PDF/A fra innsendt skjema.
 *
 * Bruker skjemadefinisjon for å hente korrekte labels/tekster
 * slik at PDF-en viser samme tekster som ble vist til bruker ved innsending.
 *
 * PDF/A-2u er påkrevd for langtidsarkivering i NAV.
 */
@Service
class PdfGeneratorService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val htmlBuilder = HtmlDokumentBuilder()

    /**
     * Genererer PDF/A fra innsendt skjema.
     *
     * @param innsendtSkjema Innsendt skjema med data og definisjon
     * @return PDF/A som byte array
     */
    fun genererPdf(innsendtSkjema: InnsendtSkjemaResponse): ByteArray {
        logger.info("Genererer PDF/A for skjema ${innsendtSkjema.skjemaId}")

        val html = htmlBuilder.byggHtml(innsendtSkjema)

        // Debug: Skriv HTML til fil for inspeksjon
        skrivDebugHtml(innsendtSkjema.referanseId, html)

        return konverterTilPdfA(html)
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
     * Konverterer HTML til PDF/A-2u ved hjelp av OpenHTMLToPDF.
     * PDF/A-2u er valgt fordi det støtter Unicode og er egnet for arkivering.
     */
    private fun konverterTilPdfA(html: String): ByteArray {
        val outputStream = ByteArrayOutputStream()

        hentFargeprofil().use { colorProfile ->
            val builder = PdfRendererBuilder()
                .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_U)
                .useColorProfile(colorProfile.readBytes())
                .withHtmlContent(html, null)
                .toStream(outputStream)

            // Registrer fonter for PDF-generering
            registrerFonter(builder)

            builder.run()
        }

        return outputStream.toByteArray()
    }

    /**
     * Registrerer fonter fra classpath for PDF-generering.
     * Bruker Liberation Sans (Apache 2.0 lisens) som er metrisk kompatibel med Arial.
     */
    private fun registrerFonter(builder: PdfRendererBuilder) {
        // Last fonter fra classpath (fungerer i distroless Docker)
        val normalFontStream = javaClass.getResourceAsStream("/pdf/fonts/LiberationSans-Regular.ttf")
            ?: throw IllegalStateException("Fant ikke LiberationSans-Regular.ttf på classpath")
        val boldFontStream = javaClass.getResourceAsStream("/pdf/fonts/LiberationSans-Bold.ttf")
            ?: throw IllegalStateException("Fant ikke LiberationSans-Bold.ttf på classpath")

        // Registrer normal font for alle font-familier brukt i CSS
        builder.useFont({ normalFontStream }, "Arial", 400, BaseRendererBuilder.FontStyle.NORMAL, true)
        builder.useFont(
            { javaClass.getResourceAsStream("/pdf/fonts/LiberationSans-Regular.ttf") },
            "Helvetica", 400, BaseRendererBuilder.FontStyle.NORMAL, true
        )
        builder.useFont(
            { javaClass.getResourceAsStream("/pdf/fonts/LiberationSans-Regular.ttf") },
            "sans-serif", 400, BaseRendererBuilder.FontStyle.NORMAL, true
        )

        // Registrer bold font
        builder.useFont({ boldFontStream }, "Arial", 700, BaseRendererBuilder.FontStyle.NORMAL, true)
        builder.useFont(
            { javaClass.getResourceAsStream("/pdf/fonts/LiberationSans-Bold.ttf") },
            "Helvetica", 700, BaseRendererBuilder.FontStyle.NORMAL, true
        )
        builder.useFont(
            { javaClass.getResourceAsStream("/pdf/fonts/LiberationSans-Bold.ttf") },
            "sans-serif", 700, BaseRendererBuilder.FontStyle.NORMAL, true
        )

        logger.debug("Registrerte Liberation Sans fonter fra classpath")
    }

    /**
     * Henter sRGB fargeprofil som er påkrevd for PDF/A.
     * Bruker den innebygde profilen fra classpath.
     */
    private fun hentFargeprofil(): InputStream {
        return javaClass.getResourceAsStream("/pdf/sRGB.icc")
            ?: throw IllegalStateException("Fant ikke sRGB fargeprofil på classpath: /pdf/sRGB.icc")
    }
}
