package no.nav.melosys.skjema.types.skjemadefinisjon

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Sealed class for feltdefinisjoner.
 * Bruker Jackson polymorfisme for å serialisere/deserialisere riktig subtype.
 *
 * @JsonTypeInfo konfigurerer at "type"-feltet brukes for å bestemme subtype.
 * Dette genererer discriminated unions i TypeScript via OpenAPI.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = BooleanFeltDefinisjon::class, name = "BOOLEAN"),
    JsonSubTypes.Type(value = TextFeltDefinisjon::class, name = "TEXT"),
    JsonSubTypes.Type(value = TextareaFeltDefinisjon::class, name = "TEXTAREA"),
    JsonSubTypes.Type(value = DateFeltDefinisjon::class, name = "DATE"),
    JsonSubTypes.Type(value = PeriodeFeltDefinisjon::class, name = "PERIOD"),
    JsonSubTypes.Type(value = SelectFeltDefinisjon::class, name = "SELECT"),
    JsonSubTypes.Type(value = CountrySelectFeltDefinisjon::class, name = "COUNTRY_SELECT"),
    JsonSubTypes.Type(value = ListeFeltDefinisjon::class, name = "LIST"),
    JsonSubTypes.Type(value = CheckboxGruppeFeltDefinisjon::class, name = "CHECKBOX_GROUP")
)
sealed class FeltDefinisjonDto {
    abstract val label: String
    abstract val hjelpetekst: String?
    abstract val pakrevd: Boolean
}

enum class FeltFormat { BELOP }

/**
 * Boolean-felt (Ja/Nei).
 *
 * @property jaLabel Visningslabel for "Ja"-valget
 * @property neiLabel Visningslabel for "Nei"-valget
 */
data class BooleanFeltDefinisjon(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = true,
    val jaLabel: String,
    val neiLabel: String
) : FeltDefinisjonDto()

/**
 * Enkelt tekstfelt (input).
 *
 * @property format Valgfritt visningsformat, f.eks. "BELOP" for beløpsfelter
 */
data class TextFeltDefinisjon(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = true,
    val format: FeltFormat? = null
) : FeltDefinisjonDto()

/**
 * Flerlinjers tekstfelt (textarea).
 *
 * @property maxLength Maksimal lengde på teksten
 */
data class TextareaFeltDefinisjon(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = true,
    val maxLength: Int? = null
) : FeltDefinisjonDto()

/**
 * Dato-felt.
 */
data class DateFeltDefinisjon(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = true
) : FeltDefinisjonDto()

/**
 * Periode-felt (fra dato - til dato).
 *
 * @property fraDatoLabel Label for fra-dato feltet
 * @property tilDatoLabel Label for til-dato feltet
 */
data class PeriodeFeltDefinisjon(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = true,
    val fraDatoLabel: String,
    val tilDatoLabel: String
) : FeltDefinisjonDto()

/**
 * Nedtrekksliste med predefinerte alternativer.
 *
 * @property alternativer Liste over mulige valg
 */
data class SelectFeltDefinisjon(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = true,
    val alternativer: List<AlternativDefinisjonDto>
) : FeltDefinisjonDto()

/**
 * Land-velger (spesialisert nedtrekksliste for land).
 * Frontend rendrer dette med en landvelger-komponent.
 */
data class CountrySelectFeltDefinisjon(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = true
) : FeltDefinisjonDto()

/**
 * Liste-felt for repeterende elementer (f.eks. familiemedlemmer, virksomheter).
 *
 * @property leggTilLabel Label for "Legg til"-knappen
 * @property fjernLabel Label for "Fjern"-knappen
 * @property tomListeMelding Melding som vises når listen er tom
 * @property elementDefinisjon Definisjon av feltene i hvert liste-element
 * @property itemTypeLabels Tittel per element-type for diskriminerte lister (f.eks. norsk/utenlandsk
 * virksomhet). Nøklene mappes mot et type-felt på hvert element i UI-flyten. Null for ikke-diskriminerte lister.
 */
data class ListeFeltDefinisjon(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = false,
    val leggTilLabel: String,
    val fjernLabel: String,
    val tomListeMelding: String? = null,
    val elementDefinisjon: Map<String, FeltDefinisjonDto>,
    val itemTypeLabels: Map<String, String>? = null
) : FeltDefinisjonDto()

/**
 * Checkbox-gruppe med predefinerte alternativer.
 *
 * @property alternativer Liste over checkbox-alternativer
 */
data class CheckboxGruppeFeltDefinisjon(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = false,
    val alternativer: List<AlternativDefinisjonDto>
) : FeltDefinisjonDto()

