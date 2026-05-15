package no.nav.melosys.skjema.pdf

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Familiemedlem
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.types.felles.NorskVirksomhet
import no.nav.melosys.skjema.types.felles.NorskeOgUtenlandskeVirksomheter
import no.nav.melosys.skjema.types.felles.NorskeOgUtenlandskeVirksomheterMedAnsettelsesform
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.felles.UtenlandskVirksomhetBase
import no.nav.melosys.skjema.types.felles.UtenlandskVirksomhetMedAnsettelsesform
import no.nav.melosys.skjema.types.skjemadefinisjon.BooleanFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.CheckboxGruppeFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.CountrySelectFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.DateFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.FeltDefinisjonDto
import no.nav.melosys.skjema.types.skjemadefinisjon.ListeFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.PeriodeFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.SelectFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.TextFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.TextareaFeltDefinisjon

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
            is CheckboxGruppeFeltDefinisjon -> renderCheckboxGruppe(felt, verdi)
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

    private fun renderBoolean(felt: BooleanFeltDefinisjon, verdi: Any): String =
        formaterVerdi(felt, verdi)?.let { renderEnkeltFelt(felt.label, it) } ?: ""

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

    private fun renderSelect(felt: SelectFeltDefinisjon, verdi: Any): String =
        formaterVerdi(felt, verdi)?.let { renderEnkeltFelt(felt.label, it) } ?: ""

    private fun renderCountry(felt: CountrySelectFeltDefinisjon, verdi: Any): String =
        formaterVerdi(felt, verdi)?.let { renderEnkeltFelt(felt.label, it) } ?: ""

    private fun renderCheckboxGruppe(felt: CheckboxGruppeFeltDefinisjon, verdi: Any): String {
        val valgteVerdier: Set<String> = when (verdi) {
            is Collection<*> -> verdi.map { it.toString() }.toSet()
            else -> return ""
        }
        val valgteLabels = felt.alternativer
            .filter { it.verdi in valgteVerdier }
            .map { it.label }
        if (valgteLabels.isEmpty()) return ""

        val listeHtml = valgteLabels.joinToString("\n") { label ->
            """<div class="checkbox-item"><span class="checkbox-icon">&#x2611;</span> ${escapeHtml(label)}</div>"""
        }

        return """
            <div class="form-summary-answer">
                <p class="form-summary-label">${escapeHtml(felt.label)}</p>
                <div class="form-summary-value checkbox-gruppe">
                    $listeHtml
                </div>
            </div>
        """.trimIndent()
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

            is NorskeOgUtenlandskeVirksomheter ->
                renderVirksomheter(felt, verdi.norskeVirksomheter, verdi.utenlandskeVirksomheter)
            is NorskeOgUtenlandskeVirksomheterMedAnsettelsesform ->
                renderVirksomheter(felt, verdi.norskeVirksomheter, verdi.utenlandskeVirksomheter)
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

    private fun renderVirksomheter(
        felt: ListeFeltDefinisjon,
        norskeVirksomheter: List<NorskVirksomhet>?,
        utenlandskeVirksomheter: List<UtenlandskVirksomhetBase>?
    ): String {
        val norske = norskeVirksomheter ?: emptyList()
        val utenlandske = utenlandskeVirksomheter ?: emptyList()

        if (norske.isEmpty() && utenlandske.isEmpty()) {
            return felt.tomListeMelding?.let { renderEnkeltFelt(felt.label, it) } ?: ""
        }

        val ed = felt.elementDefinisjon
        val itemTypeLabels = requireNotNull(felt.itemTypeLabels) {
            "Mangler itemTypeLabels i definisjonen for virksomhetsliste '${felt.label}'"
        }
        val builder = StringBuilder()
        builder.append("""<div class="list-container">""")
        builder.append("""<div class="list-label">${escapeHtml(felt.label)}</div>""")

        var index = 1

        norske.forEach { virksomhet ->
            builder.append("""<div class="list-item">""")
            builder.append("""<div class="list-item-title">${index++}. ${escapeHtml(itemTypeLabels.getValue("norsk"))}</div>""")
            builder.append(renderListeFelt(ed.getValue("organisasjonsnummer"), virksomhet.organisasjonsnummer))
            builder.append("</div>")
        }

        utenlandske.forEach { virksomhet ->
            builder.append("""<div class="list-item">""")
            builder.append("""<div class="list-item-title">${index++}. ${escapeHtml(itemTypeLabels.getValue("utenlandsk"))}</div>""")
            builder.append(renderListeFelt(ed.getValue("navn"), virksomhet.navn))
            builder.append(renderListeFelt(ed.getValue("organisasjonsnummer"), virksomhet.organisasjonsnummer))
            builder.append(renderListeFelt(ed.getValue("vegnavnOgHusnummer"), virksomhet.vegnavnOgHusnummer))
            builder.append(renderListeFelt(ed.getValue("bygning"), virksomhet.bygning))
            builder.append(renderListeFelt(ed.getValue("postkode"), virksomhet.postkode))
            builder.append(renderListeFelt(ed.getValue("byStedsnavn"), virksomhet.byStedsnavn))
            builder.append(renderListeFelt(ed.getValue("region"), virksomhet.region))
            builder.append(renderListeFelt(ed.getValue("land"), virksomhet.land))
            builder.append(renderListeFelt(ed.getValue("tilhorerSammeKonsern"), virksomhet.tilhorerSammeKonsern))
            if (virksomhet is UtenlandskVirksomhetMedAnsettelsesform) {
                builder.append(renderListeFelt(ed.getValue("ansettelsesform"), virksomhet.ansettelsesform))
            }
            builder.append("</div>")
        }

        builder.append("</div>")
        return builder.toString()
    }

    private fun renderListeFelt(feltDef: FeltDefinisjonDto, verdi: Any?): String {
        if (verdi == null) return ""
        val visningsverdi = formaterVerdi(feltDef, verdi) ?: return ""
        return renderListeElement(feltDef.label, visningsverdi)
    }

    private fun formaterVerdi(feltDef: FeltDefinisjonDto, verdi: Any): String? = when (feltDef) {
        is BooleanFeltDefinisjon -> (verdi as? Boolean)?.let { if (it) feltDef.jaLabel else feltDef.neiLabel }
        is SelectFeltDefinisjon -> {
            val verdiStr = if (verdi is Enum<*>) verdi.name else verdi.toString()
            feltDef.alternativer.find { it.verdi == verdiStr }?.label ?: verdiStr
        }
        is CountrySelectFeltDefinisjon -> when (verdi) {
            is LandKode -> verdi.hentNavn(språk)
            is String -> LandKode.hentLandnavn(verdi, språk)
            else -> verdi.toString()
        }
        else -> verdi.toString()
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
