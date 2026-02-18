package no.nav.melosys.skjema.pdf

/**
 * CSS-styling for PDF-generering.
 * Basert på NAV Aksel FormSummary-komponenten.
 *
 * NB: Bruker direkte verdier (ikke CSS custom properties) for kompatibilitet med OpenHTMLToPDF.
 */
object PdfStyles {

    val CSS = """
        /* NAV Aksel-inspirert styling - direkte verdier for PDF-kompatibilitet */
        body {
            font-family: Arial, Helvetica, sans-serif;
            font-size: 11pt;
            line-height: 1.5;
            color: #262626;
            max-width: 700px;
            margin: 0 auto;
            padding: 24px;
            background: #FFFFFF;
        }

        /* Hovedtittel */
        .document-header {
            margin-bottom: 32px;
            padding-bottom: 24px;
            border-bottom: 4px solid #0067C5;
        }
        .document-title {
            font-size: 22pt;
            font-weight: bold;
            color: #262626;
            margin: 0 0 12px 0;
        }
        .document-meta {
            font-size: 10pt;
            color: #59514B;
        }
        .document-meta p {
            margin: 4px 0;
        }

        /* Del-overskrift (Arbeidstakers del / Arbeidsgivers del) */
        .part-heading {
            font-size: 16pt;
            font-weight: bold;
            color: #262626;
            margin: 32px 0 16px 0;
            padding-bottom: 8px;
            border-bottom: 2px solid #C6C2BF;
        }

        /* FormSummary-stil seksjon */
        .form-summary {
            background: #FFFFFF;
            border: 1px solid #C6C2BF;
            margin-bottom: 24px;
        }
        .form-summary-header {
            background: #F7F7F7;
            padding: 12px 16px;
            border-bottom: 1px solid #C6C2BF;
        }
        .form-summary-heading {
            font-size: 12pt;
            font-weight: bold;
            color: #262626;
            margin: 0;
        }
        .form-summary-answers {
            padding: 0;
        }

        /* Enkelt felt/svar */
        .form-summary-answer {
            padding: 10px 16px;
            border-bottom: 1px solid #E6E4E2;
        }
        .form-summary-answer p {
            margin: 0;
        }
        .form-summary-label {
            font-size: 10pt;
            font-weight: bold;
            color: #59514B;
            margin: 0 0 2px 0;
        }
        .form-summary-value {
            font-size: 11pt;
            color: #262626;
            margin: 0;
        }

        /* Liste-elementer (familiemedlemmer, virksomheter) */
        .list-container {
            padding: 10px 16px;
            border-bottom: 1px solid #E6E4E2;
        }
        .list-label {
            font-size: 10pt;
            font-weight: bold;
            color: #59514B;
            display: block;
            margin-bottom: 8px;
        }
        .list-item {
            background: #F7F7F7;
            border: 1px solid #E6E4E2;
            padding: 10px;
            margin-bottom: 8px;
        }
        .list-item-title {
            font-size: 9pt;
            font-weight: bold;
            color: #59514B;
            margin-bottom: 6px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .list-item-field {
            margin-bottom: 4px;
        }
        .list-item-label {
            font-size: 9pt;
            color: #59514B;
            display: inline;
        }
        .list-item-value {
            font-size: 10pt;
            color: #262626;
            display: inline;
            margin-left: 4px;
        }
    """.trimIndent()
}
