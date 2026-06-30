package no.nav.melosys.skjema.pdf

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerDokumentTittel
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
    private val norskDatoFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val navLogoDataUri: String by lazy {
        val logoBytes = object {}.javaClass.getResourceAsStream("/pdf/nav-logo.png")?.readBytes()
            ?: throw IllegalStateException("Fant ikke NAV-logo på classpath: /pdf/nav-logo.png")
        "data:image/png;base64,${Base64.getEncoder().encodeToString(logoBytes)}"
    }

    /**
     * Bygger komplett HTML-dokument fra skjemadata.
     * Bruker språket fra skjemaet for å vise riktige tekster.
     */
    fun byggHtml(skjema: SkjemaPdfData): String {
        val språk = skjema.innsendtSprak
        val feltRenderer = FeltRenderer(språk)
        val primaryRenderer = SeksjonRenderer(feltRenderer, skjema.vedlegg)
        val tittel = UtsendtArbeidstakerDokumentTittel.utled(skjema.skjemaData, språk)

        return buildString {
            append(byggHtmlStart())
            append(byggHeader(tittel, skjema.referanseId, skjema.innsendtDato, språk))
            append(byggAktørInfoSeksjon(skjema.aktørInfo, skjema.fullmektigInfo, skjema.radgiverInfo, språk))
            appendSkjemaData(skjema.skjemaData, skjema.definisjon, primaryRenderer, språk)
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

    private fun byggHeader(tittel: String, referanseId: String, innsendtDato: Instant, språk: Språk): String {
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
                <img class="nav-logo" src="$navLogoDataUri" alt="NAV" />
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

    private fun byggAktørInfoSeksjon(aktørInfo: AktørInfo, fullmektigInfo: FullmektigInfo?, radgiverInfo: RadgiverInfo?, språk: Språk): String {
        val aktørerOverskrift = when (språk) {
            Språk.NORSK_BOKMAL -> "Aktører"
            Språk.ENGELSK -> "Parties"
        }
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
        val virksomhetsnavnTekst = when (språk) {
            Språk.NORSK_BOKMAL -> "Virksomhetsnavn"
            Språk.ENGELSK -> "Company name"
        }

        return buildString {
            append("""<h2 class="part-heading">${escapeHtml(aktørerOverskrift)}</h2>""")
            append("""
            <div class="form-summary">
                <div class="form-summary-header">
                    <h3 class="form-summary-heading">${escapeHtml(arbeidstakerRolle)}</h3>
                </div>
                <div class="form-summary-answers">
                    ${byggFormSummarySvar(navnTekst, aktørInfo.arbeidstakerNavn)}
                    ${byggFormSummarySvar(identTekst, aktørInfo.arbeidstakerFnr)}
                    ${fullmektigInfo?.let {
                        val fullmektigTekst = when (språk) {
                            Språk.NORSK_BOKMAL -> "Fullmektig"
                            Språk.ENGELSK -> "Power of attorney"
                        }
                        byggFormSummarySvar(fullmektigTekst, "${it.navn} (${norskDatoFormatter.format(it.foedselsdato)})")
                    }.orEmpty()}
                </div>
            </div>
            """.trimIndent())

            append("""
            <div class="form-summary">
                <div class="form-summary-header">
                    <h3 class="form-summary-heading">${escapeHtml(arbeidsgiverRolle)}</h3>
                </div>
                <div class="form-summary-answers">
                    ${byggFormSummarySvar(virksomhetsnavnTekst, aktørInfo.arbeidsgiverNavn)}
                    ${byggFormSummarySvar(orgnrTekst, aktørInfo.orgnr)}
                    ${radgiverInfo?.let {
                        val radgiverFirmaTekst = when (språk) {
                            Språk.NORSK_BOKMAL -> "Rådgiverfirma"
                            Språk.ENGELSK -> "Advisory firm"
                        }
                        val kontaktpersonTekst = when (språk) {
                            Språk.NORSK_BOKMAL -> "Kontaktperson rådgiverfirma"
                            Språk.ENGELSK -> "Contact person at advisory firm"
                        }
                        byggFormSummarySvar(radgiverFirmaTekst, it.firmaNavn) + "\n" +
                            byggFormSummarySvar(kontaktpersonTekst, it.personNavn)
                    } ?: aktørInfo.kontaktpersonNavn?.let {
                        val kontaktpersonTekst = when (språk) {
                            Språk.NORSK_BOKMAL -> "Kontaktperson"
                            Språk.ENGELSK -> "Contact person"
                        }
                        byggFormSummarySvar(kontaktpersonTekst, it)
                    }.orEmpty()}
                </div>
            </div>
            """.trimIndent())
        }
    }

    private fun byggFormSummarySvar(label: String, verdi: String): String = """
        <div class="form-summary-answer">
            <p class="form-summary-label">${escapeHtml(label)}</p>
            <p class="form-summary-value">${escapeHtml(verdi)}</p>
        </div>
    """.trimIndent()

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
