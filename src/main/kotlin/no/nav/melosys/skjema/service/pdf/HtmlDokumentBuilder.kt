package no.nav.melosys.skjema.service.pdf

import no.nav.melosys.skjema.dto.InnsendtSkjemaResponse
import no.nav.melosys.skjema.service.skjemadefinisjon.Språk
import java.time.format.DateTimeFormatter

/**
 * Bygger HTML-dokument fra innsendt skjema.
 * Bruker typede DTO-er og skjemadefinisjon for korrekte labels.
 * Ingen Map-konvertering - direkte tilgang til felter.
 */
class HtmlDokumentBuilder {
    private val datoTidFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .withZone(java.time.ZoneId.of("Europe/Oslo"))

    /**
     * Bygger komplett HTML-dokument fra skjemadata.
     * Bruker språket fra skjemaet for å vise riktige tekster.
     */
    fun byggHtml(skjema: InnsendtSkjemaResponse): String {
        // Opprett renderere med riktig språk fra skjemaet
        val språk = skjema.innsendtSprak
        val feltRenderer = FeltRenderer(språk)
        val seksjonRenderer = SeksjonRenderer(feltRenderer)

        val builder = StringBuilder()

        builder.append(byggHtmlStart())
        builder.append(byggHeader(skjema, språk))
        builder.append(byggArbeidstakerDel(skjema, seksjonRenderer, språk))
        builder.append(byggArbeidsgiverDel(skjema, seksjonRenderer, språk))
        builder.append(byggHtmlSlutt())

        return builder.toString()
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

    private fun byggHeader(skjema: InnsendtSkjemaResponse, språk: Språk): String {
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
                    <p><strong>${escapeHtml(referanseTekst)}:</strong> ${escapeHtml(skjema.referanseId)}</p>
                    <p><strong>${escapeHtml(innsendtTekst)}:</strong> ${datoTidFormatter.format(skjema.innsendtDato)}</p>
                </div>
            </div>
        """.trimIndent()
    }

    private fun byggArbeidstakerDel(
        skjema: InnsendtSkjemaResponse,
        seksjonRenderer: SeksjonRenderer,
        språk: Språk
    ): String {
        val arbeidstakerData = skjema.arbeidstakerData ?: return ""

        val overskrift = when (språk) {
            Språk.NORSK_BOKMAL -> "Arbeidstakers del"
            Språk.ENGELSK -> "Employee's section"
        }

        val builder = StringBuilder()
        builder.append("""<h2 class="part-heading">${escapeHtml(overskrift)}</h2>""")
        builder.append(seksjonRenderer.byggArbeidstakerSeksjoner(arbeidstakerData, skjema.definisjon))

        return builder.toString()
    }

    private fun byggArbeidsgiverDel(
        skjema: InnsendtSkjemaResponse,
        seksjonRenderer: SeksjonRenderer,
        språk: Språk
    ): String {
        val arbeidsgiverData = skjema.arbeidsgiverData ?: return ""

        val overskrift = when (språk) {
            Språk.NORSK_BOKMAL -> "Arbeidsgivers del"
            Språk.ENGELSK -> "Employer's section"
        }

        val builder = StringBuilder()
        builder.append("""<h2 class="part-heading">${escapeHtml(overskrift)}</h2>""")
        builder.append(seksjonRenderer.byggArbeidsgiverSeksjoner(arbeidsgiverData, skjema.definisjon))

        return builder.toString()
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
