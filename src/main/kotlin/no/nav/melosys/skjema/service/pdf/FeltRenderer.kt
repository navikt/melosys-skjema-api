package no.nav.melosys.skjema.service.pdf

import no.nav.melosys.skjema.types.arbeidstaker.familiemedlemmer.Familiemedlem
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.NorskeOgUtenlandskeVirksomheter
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.skjemadefinisjon.*
import no.nav.melosys.skjema.types.common.Språk
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Rendrer felt til HTML basert på feltdefinisjon og typet verdi.
 * Ingen Map-konvertering - jobber direkte med typede verdier.
 */
class FeltRenderer(
    private val språk: Språk = Språk.NORSK_BOKMAL
) {
    private val datoFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    /**
     * Rendrer et felt basert på felttype og verdi.
     */
    fun render(felt: FeltDefinisjonDto, verdi: Any): String {
        return when (felt) {
            is BooleanFeltDefinisjon -> renderBoolean(felt, verdi)
            is TextFeltDefinisjon -> renderText(felt, verdi)
            is TextareaFeltDefinisjon -> renderTextarea(felt, verdi)
            is DateFeltDefinisjon -> renderDate(felt, verdi)
            is PeriodeFeltDefinisjon -> renderPeriode(felt, verdi)
            is SelectFeltDefinisjon -> renderSelect(felt, verdi)
            is CountrySelectFeltDefinisjon -> renderCountry(felt, verdi)
            is ListeFeltDefinisjon -> renderListe(felt, verdi)
        }
    }

    fun renderEnkeltFelt(label: String, verdi: String): String {
        return """
            <div class="form-summary-answer">
                <p class="form-summary-label">${escapeHtml(label)}</p>
                <p class="form-summary-value">${escapeHtml(verdi)}</p>
            </div>
        """.trimIndent()
    }

    private fun renderBoolean(felt: BooleanFeltDefinisjon, verdi: Any): String {
        val boolVerdi = when (verdi) {
            is Boolean -> verdi
            else -> return ""
        }
        val visningsverdi = if (boolVerdi) felt.jaLabel else felt.neiLabel
        return renderEnkeltFelt(felt.label, visningsverdi)
    }

    private fun renderText(felt: TextFeltDefinisjon, verdi: Any): String {
        val tekst = verdi.toString()
        if (tekst.isBlank()) return ""
        return renderEnkeltFelt(felt.label, tekst)
    }

    private fun renderTextarea(felt: TextareaFeltDefinisjon, verdi: Any): String {
        val tekst = verdi.toString()
        if (tekst.isBlank()) return ""
        return renderEnkeltFelt(felt.label, tekst)
    }

    private fun renderDate(felt: DateFeltDefinisjon, verdi: Any): String {
        val dato = when (verdi) {
            is LocalDate -> verdi.format(datoFormatter)
            else -> verdi.toString()
        }
        return renderEnkeltFelt(felt.label, dato)
    }

    private fun renderPeriode(felt: PeriodeFeltDefinisjon, verdi: Any): String {
        val periode = verdi as? PeriodeDto ?: return ""
        val fraDato = periode.fraDato.format(datoFormatter)
        val tilDato = periode.tilDato.format(datoFormatter)

        return renderEnkeltFelt(felt.fraDatoLabel, fraDato) + "\n" +
                renderEnkeltFelt(felt.tilDatoLabel, tilDato)
    }

    private fun renderSelect(felt: SelectFeltDefinisjon, verdi: Any): String {
        val verdiStr = when (verdi) {
            is Enum<*> -> verdi.name
            else -> verdi.toString()
        }
        val visningsverdi = felt.alternativer.find { it.verdi == verdiStr }?.label ?: verdiStr
        return renderEnkeltFelt(felt.label, visningsverdi)
    }

    private fun renderCountry(felt: CountrySelectFeltDefinisjon, verdi: Any): String {
        val landnavn = when (verdi) {
            is LandKode -> verdi.hentNavn(språk)
            else -> verdi.toString()
        }
        return renderEnkeltFelt(felt.label, landnavn)
    }

    @Suppress("UNCHECKED_CAST")
    private fun renderListe(felt: ListeFeltDefinisjon, verdi: Any): String {
        // Håndter spesifikke listetyper
        return when (verdi) {
            is List<*> -> {
                if (verdi.isEmpty()) {
                    felt.tomListeMelding?.let { renderEnkeltFelt(felt.label, it) } ?: ""
                } else {
                    when (val første = verdi.firstOrNull()) {
                        is Familiemedlem -> renderFamiliemedlemmer(felt, verdi as List<Familiemedlem>)
                        else -> renderGeneriskListe(felt, verdi)
                    }
                }
            }

            is NorskeOgUtenlandskeVirksomheter -> renderVirksomheter(felt, verdi)
            else -> ""
        }
    }

    private fun renderFamiliemedlemmer(felt: ListeFeltDefinisjon, liste: List<Familiemedlem>): String {
        val builder = StringBuilder()
        builder.append("""<div class="list-container">""")
        builder.append("""<div class="list-label">${escapeHtml(felt.label)}</div>""")

        liste.forEachIndexed { index, medlem ->
            builder.append("""<div class="list-item">""")
            builder.append("""<div class="list-item-title">${index + 1}. familiemedlem</div>""")

            felt.elementDefinisjon["fornavn"]?.let { feltDef ->
                builder.append(renderListeElement(feltDef.label, medlem.fornavn))
            }
            felt.elementDefinisjon["etternavn"]?.let { feltDef ->
                builder.append(renderListeElement(feltDef.label, medlem.etternavn))
            }

            if (medlem.harNorskFodselsnummerEllerDnummer) {
                medlem.fodselsnummer?.let { fnr ->
                    felt.elementDefinisjon["fodselsnummer"]?.let { feltDef ->
                        builder.append(renderListeElement(feltDef.label, fnr))
                    }
                }
            } else {
                medlem.fodselsdato?.let { dato ->
                    felt.elementDefinisjon["fodselsdato"]?.let { feltDef ->
                        builder.append(renderListeElement(feltDef.label, dato.format(datoFormatter)))
                    }
                }
            }

            builder.append("</div>")
        }

        builder.append("</div>")
        return builder.toString()
    }

    private fun renderVirksomheter(felt: ListeFeltDefinisjon, virksomheter: NorskeOgUtenlandskeVirksomheter): String {
        val norske = virksomheter.norskeVirksomheter ?: emptyList()
        val utenlandske = virksomheter.utenlandskeVirksomheter ?: emptyList()

        if (norske.isEmpty() && utenlandske.isEmpty()) {
            return felt.tomListeMelding?.let { renderEnkeltFelt(felt.label, it) } ?: ""
        }

        val builder = StringBuilder()
        builder.append("""<div class="list-container">""")
        builder.append("""<div class="list-label">${escapeHtml(felt.label)}</div>""")

        var index = 1

        norske.forEach { virksomhet ->
            builder.append("""<div class="list-item">""")
            builder.append("""<div class="list-item-title">${index++}. virksomhet (norsk)</div>""")
            builder.append(renderListeElement("Organisasjonsnummer", virksomhet.organisasjonsnummer))
            builder.append("</div>")
        }

        utenlandske.forEach { virksomhet ->
            builder.append("""<div class="list-item">""")
            builder.append("""<div class="list-item-title">${index++}. virksomhet (utenlandsk)</div>""")
            builder.append(renderListeElement("Navn", virksomhet.navn))
            virksomhet.organisasjonsnummer?.let { builder.append(renderListeElement("Organisasjonsnummer", it)) }
            // land er en String landkode, konverter til landnavn
            val landnavn = LandKode.hentLandnavn(virksomhet.land, språk)
            builder.append(renderListeElement("Land", landnavn))
            builder.append("</div>")
        }

        builder.append("</div>")
        return builder.toString()
    }

    private fun renderGeneriskListe(felt: ListeFeltDefinisjon, liste: List<*>): String {
        // Fallback for ukjente listetyper
        return renderEnkeltFelt(felt.label, "${liste.size} elementer")
    }

    private fun renderListeElement(label: String, verdi: String): String {
        return """
            <div class="list-item-field">
                <span class="list-item-label">${escapeHtml(label)}</span>
                <span class="list-item-value">${escapeHtml(verdi)}</span>
            </div>
        """.trimIndent()
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
