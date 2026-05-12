package no.nav.melosys.skjema.pdf

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
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
            append(byggAktørInfoSeksjon(skjema.aktørInfo, språk))
            appendSkjemaData(skjema.skjemaData, skjema.definisjon, seksjonRenderer, språk)
            skjema.kobletSkjemaData?.let { appendSkjemaData(it, skjema.definisjon, seksjonRenderer, språk) }
            append(byggHtmlSlutt())
        }
    }

    private fun StringBuilder.appendSkjemaData(
        data: UtsendtArbeidstakerSkjemaData,
        definisjon: SkjemaDefinisjonDto,
        seksjonRenderer: SeksjonRenderer,
        språk: Språk
    ) {
        when (data) {
            is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto ->
                append(byggArbeidsgiverDel(data, definisjon, seksjonRenderer, språk))
            is UtsendtArbeidstakerArbeidstakersSkjemaDataDto ->
                append(byggArbeidstakerDel(data, definisjon, seksjonRenderer, språk))
            is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto ->
                append(byggKombinertDel(data, definisjon, seksjonRenderer, språk))
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

    private fun byggArbeidsgiverDel(
        data: UtsendtArbeidstakerArbeidsgiversSkjemaDataDto,
        definisjon: SkjemaDefinisjonDto,
        seksjonRenderer: SeksjonRenderer,
        språk: Språk
    ): String {
        val overskrift = arbeidsgiverOverskrift(språk)

        return buildString {
            append("""<h2 class="part-heading">${escapeHtml(overskrift)}</h2>""")
            append(seksjonRenderer.byggArbeidsgiverSeksjoner(data, definisjon))
        }
    }

    private fun byggArbeidstakerDel(
        data: UtsendtArbeidstakerArbeidstakersSkjemaDataDto,
        definisjon: SkjemaDefinisjonDto,
        seksjonRenderer: SeksjonRenderer,
        språk: Språk
    ): String {
        val overskrift = arbeidstakerOverskrift(språk)

        return buildString {
            append("""<h2 class="part-heading">${escapeHtml(overskrift)}</h2>""")
            append(seksjonRenderer.byggArbeidstakerSeksjoner(data, definisjon))
        }
    }

    private fun byggKombinertDel(
        data: UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto,
        definisjon: SkjemaDefinisjonDto,
        seksjonRenderer: SeksjonRenderer,
        språk: Språk
    ): String {
        return buildString {
            append("""<h2 class="part-heading">${escapeHtml(arbeidsgiverOverskrift(språk))}</h2>""")
            append(seksjonRenderer.byggKombinertArbeidsgiversSeksjoner(data, definisjon))
            append("""<h2 class="part-heading">${escapeHtml(arbeidstakerOverskrift(språk))}</h2>""")
            append(seksjonRenderer.byggKombinertArbeidstakersSeksjoner(data, definisjon))
        }
    }

    private fun byggAktørInfoSeksjon(aktørInfo: AktørInfo, språk: Språk): String {
        val arbeidstakerRolle = when (språk) {
            Språk.NORSK_BOKMAL -> "Arbeidstaker"
            Språk.ENGELSK -> "Employee"
        }
        val arbeidsgiverRolle = when (språk) {
            Språk.NORSK_BOKMAL -> "Arbeidsgiver"
            Språk.ENGELSK -> "Employer"
        }
        val orgnrTekst = when (språk) {
            Språk.NORSK_BOKMAL -> "Organisasjonsnummer"
            Språk.ENGELSK -> "Organisation number"
        }
        val identTekst = personidentifikatorLabel(aktørInfo.arbeidstakerFnr, språk)
        val navnTekst = when (språk) {
            Språk.NORSK_BOKMAL -> "Navn"
            Språk.ENGELSK -> "Name"
        }

        return """
            <div class="form-summary">
                <div class="form-summary-header">
                    <h3 class="form-summary-heading">${escapeHtml(arbeidstakerRolle)}</h3>
                </div>
                <div class="form-summary-answers">
                    <div class="form-summary-answer">
                        <p class="form-summary-label">${escapeHtml(navnTekst)}</p>
                        <p class="form-summary-value">${escapeHtml(aktørInfo.arbeidstakerNavn)}</p>
                    </div>
                    <div class="form-summary-answer">
                        <p class="form-summary-label">${escapeHtml(identTekst)}</p>
                        <p class="form-summary-value">${escapeHtml(aktørInfo.arbeidstakerFnr)}</p>
                    </div>
                </div>
            </div>
            <div class="form-summary">
                <div class="form-summary-header">
                    <h3 class="form-summary-heading">${escapeHtml(arbeidsgiverRolle)}</h3>
                </div>
                <div class="form-summary-answers">
                    <div class="form-summary-answer">
                        <p class="form-summary-label">${escapeHtml(navnTekst)}</p>
                        <p class="form-summary-value">${escapeHtml(aktørInfo.arbeidsgiverNavn)}</p>
                    </div>
                    <div class="form-summary-answer">
                        <p class="form-summary-label">${escapeHtml(orgnrTekst)}</p>
                        <p class="form-summary-value">${escapeHtml(aktørInfo.orgnr)}</p>
                    </div>
                </div>
            </div>
        """.trimIndent()
    }

    /**
     * Utleder riktig label for personidentifikator basert på om det er et D-nummer eller fødselsnummer.
     * D-nummer gjenkjennes ved at første siffer er 4–7 (dag-delen er økt med 4).
     */
    private fun personidentifikatorLabel(ident: String, språk: Språk): String {
        val erDnummer = ident.length == 11 && ident[0].digitToInt() in 4..7
        return when {
            erDnummer && språk == Språk.NORSK_BOKMAL -> "D-nummer"
            erDnummer && språk == Språk.ENGELSK -> "D number"
            språk == Språk.NORSK_BOKMAL -> "Fødselsnummer"
            else -> "National identity number"
        }
    }

    private fun arbeidsgiverOverskrift(språk: Språk): String = when (språk) {
        Språk.NORSK_BOKMAL -> "Arbeidsgivers del"
        Språk.ENGELSK -> "Employer's section"
    }

    private fun arbeidstakerOverskrift(språk: Språk): String = when (språk) {
        Språk.NORSK_BOKMAL -> "Arbeidstakers del"
        Språk.ENGELSK -> "Employee's section"
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
