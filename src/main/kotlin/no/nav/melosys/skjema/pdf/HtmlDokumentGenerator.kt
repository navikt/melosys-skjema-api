package no.nav.melosys.skjema.pdf

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import no.nav.melosys.skjema.types.arbeidsgiver.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.skjemadefinisjon.SkjemaDefinisjonDto

/**
 * Genererer HTML-dokument fra skjemadata for PDF-generering.
 */
object HtmlDokumentGenerator {

    private val datoTidFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .withZone(ZoneId.of("Europe/Oslo"))

    /**
     * Bygger komplett HTML-dokument fra skjemadata.
     * Bruker språket fra skjemaet for å vise riktige tekster.
     */
    fun byggHtml(skjema: SkjemaPdfData): String {
        val språk = skjema.innsendtSprak
        val feltRenderer = FeltRenderer(språk)
        val seksjonRenderer = SeksjonRenderer(feltRenderer)

        return buildString {
            append(byggHtmlStart())
            append(byggHeader(skjema.referanseId, skjema.innsendtDato, språk))
            append(byggArbeidsgiverDel(skjema.arbeidsgiverData, skjema.definisjon, seksjonRenderer, språk))
            append(byggArbeidstakerDel(skjema.arbeidstakerData, skjema.definisjon, seksjonRenderer, språk))
            append(byggHtmlSlutt())
        }
    }

    private fun byggHtmlStart(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8"/>
                <style>
                ${PdfStyles.CSS}
                </style>
            </head>
            <body>
        """.trimIndent()
    }

    private fun byggHtmlSlutt(): String = "</body></html>"

    private fun byggHeader(referanseId: String, innsendtDato: Instant, språk: Språk): String {
        val tittel = when (språk) {
            Språk.NORSK_BOKMAL -> "Søknad om A1 for utsendte arbeidstakere i EØS/Sveits"
            Språk.ENGELSK -> "Application for posted worker within EU/EEA and Switzerland"
        }

        val referanseTekst = when (språk) {
            Språk.NORSK_BOKMAL -> "Referansenummer"
            Språk.ENGELSK -> "Reference number"
        }

        val innsendtTekst = when (språk) {
            Språk.NORSK_BOKMAL -> "Innsendt"
            Språk.ENGELSK -> "Submitted"
        }

        return """
            <div class="document-header">
                <h1 class="document-title">${escapeHtml(tittel)}</h1>
                <div class="document-meta">
                    <p><strong>${escapeHtml(referanseTekst)}:</strong> ${escapeHtml(referanseId)}</p>
                    <p><strong>${escapeHtml(innsendtTekst)}:</strong> ${datoTidFormatter.format(innsendtDato)}</p>
                </div>
            </div>
        """.trimIndent()
    }

    private fun byggArbeidstakerDel(
        arbeidstakerData: UtsendtArbeidstakerArbeidstakersSkjemaDataDto?,
        definisjon: SkjemaDefinisjonDto,
        seksjonRenderer: SeksjonRenderer,
        språk: Språk
    ): String {
        if (arbeidstakerData == null) return ""

        val overskrift = when (språk) {
            Språk.NORSK_BOKMAL -> "Arbeidstakers del"
            Språk.ENGELSK -> "Employee's section"
        }

        return buildString {
            append("""<h2 class="part-heading">${escapeHtml(overskrift)}</h2>""")
            append(seksjonRenderer.byggArbeidstakerSeksjoner(arbeidstakerData, definisjon))
        }
    }

    private fun byggArbeidsgiverDel(
        arbeidsgiverData: UtsendtArbeidstakerArbeidsgiversSkjemaDataDto?,
        definisjon: SkjemaDefinisjonDto,
        seksjonRenderer: SeksjonRenderer,
        språk: Språk
    ): String {
        if (arbeidsgiverData == null) return ""

        val overskrift = when (språk) {
            Språk.NORSK_BOKMAL -> "Arbeidsgivers del"
            Språk.ENGELSK -> "Employer's section"
        }

        return buildString {
            append("""<h2 class="part-heading">${escapeHtml(overskrift)}</h2>""")
            append(seksjonRenderer.byggArbeidsgiverSeksjoner(arbeidsgiverData, definisjon))
        }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
