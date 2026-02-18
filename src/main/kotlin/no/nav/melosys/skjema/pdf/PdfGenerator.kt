package no.nav.melosys.skjema.pdf

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

private val log = KotlinLogging.logger {}

/**
 * Bygger HTML fra skjemadata for PDF-generering.
 */
private fun byggHtml(skjemaPdfData: SkjemaPdfData): String =
    HtmlDokumentGenerator.byggHtml(skjemaPdfData)

/**
 * Genererer PDF/A fra skjemadata.
 *
 * PDF/A-2u er påkrevd for langtidsarkivering i NAV.
 *
 * @param skjemaPdfData Data for PDF-generering
 * @return PDF/A som byte array
 */
fun genererPdf(skjemaPdfData: SkjemaPdfData): ByteArray {
    log.info { "Genererer PDF/A for skjema ${skjemaPdfData.skjemaId}" }

    val html = byggHtml(skjemaPdfData)

    skrivDebugHtml(skjemaPdfData.referanseId, html)

    return konverterTilPdfA(html)
}

private fun skrivDebugHtml(referanseId: String, html: String) {
    try {
        File("build/test-output/debug-$referanseId.html").apply {
            parentFile?.mkdirs()
            writeText(html)
        }
    } catch (e: Exception) {
        log.debug { "Kunne ikke skrive debug-HTML: ${e.message}" }
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
    val normalFontStream = hentRessurs("/pdf/fonts/LiberationSans-Regular.ttf")
        ?: throw IllegalStateException("Fant ikke LiberationSans-Regular.ttf på classpath")
    val boldFontStream = hentRessurs("/pdf/fonts/LiberationSans-Bold.ttf")
        ?: throw IllegalStateException("Fant ikke LiberationSans-Bold.ttf på classpath")

    // Registrer normal font for alle font-familier brukt i CSS
    builder.useFont({ normalFontStream }, "Arial", 400, BaseRendererBuilder.FontStyle.NORMAL, true)
    builder.useFont(
        { hentRessurs("/pdf/fonts/LiberationSans-Regular.ttf") },
        "Helvetica", 400, BaseRendererBuilder.FontStyle.NORMAL, true
    )
    builder.useFont(
        { hentRessurs("/pdf/fonts/LiberationSans-Regular.ttf") },
        "sans-serif", 400, BaseRendererBuilder.FontStyle.NORMAL, true
    )

    // Registrer bold font
    builder.useFont({ boldFontStream }, "Arial", 700, BaseRendererBuilder.FontStyle.NORMAL, true)
    builder.useFont(
        { hentRessurs("/pdf/fonts/LiberationSans-Bold.ttf") },
        "Helvetica", 700, BaseRendererBuilder.FontStyle.NORMAL, true
    )
    builder.useFont(
        { hentRessurs("/pdf/fonts/LiberationSans-Bold.ttf") },
        "sans-serif", 700, BaseRendererBuilder.FontStyle.NORMAL, true
    )

    log.debug { "Registrerte Liberation Sans fonter fra classpath" }
}

/**
 * Henter sRGB fargeprofil som er påkrevd for PDF/A.
 */
private fun hentFargeprofil(): InputStream {
    return hentRessurs("/pdf/sRGB.icc")
        ?: throw IllegalStateException("Fant ikke sRGB fargeprofil på classpath: /pdf/sRGB.icc")
}

private fun hentRessurs(path: String): InputStream? {
    return object {}.javaClass.getResourceAsStream(path)
}
