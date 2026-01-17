package no.nav.melosys.skjema.dto.skjemadefinisjon

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
    JsonSubTypes.Type(value = BooleanFeltDto::class, name = "BOOLEAN"),
    JsonSubTypes.Type(value = TextFeltDto::class, name = "TEXT"),
    JsonSubTypes.Type(value = TextareaFeltDto::class, name = "TEXTAREA"),
    JsonSubTypes.Type(value = DateFeltDto::class, name = "DATE"),
    JsonSubTypes.Type(value = PeriodeFeltDto::class, name = "PERIOD"),
    JsonSubTypes.Type(value = SelectFeltDto::class, name = "SELECT"),
    JsonSubTypes.Type(value = CountrySelectFeltDto::class, name = "COUNTRY_SELECT"),
    JsonSubTypes.Type(value = ListeFeltDto::class, name = "LIST")
)
sealed class FeltDto {
    abstract val label: String
    abstract val hjelpetekst: String?
    abstract val pakrevd: Boolean
}

/**
 * Boolean-felt (Ja/Nei).
 *
 * @property jaLabel Visningslabel for "Ja"-valget
 * @property neiLabel Visningslabel for "Nei"-valget
 */
data class BooleanFeltDto(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = true,
    val jaLabel: String = "Ja",
    val neiLabel: String = "Nei"
) : FeltDto()

/**
 * Enkelt tekstfelt (input).
 */
data class TextFeltDto(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = true
) : FeltDto()

/**
 * Flerlinjers tekstfelt (textarea).
 *
 * @property maxLength Maksimal lengde på teksten
 */
data class TextareaFeltDto(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = true,
    val maxLength: Int? = null
) : FeltDto()

/**
 * Dato-felt.
 */
data class DateFeltDto(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = true
) : FeltDto()

/**
 * Periode-felt (fra dato - til dato).
 *
 * @property fraDatoLabel Label for fra-dato feltet
 * @property tilDatoLabel Label for til-dato feltet
 */
data class PeriodeFeltDto(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = true,
    val fraDatoLabel: String = "Fra dato",
    val tilDatoLabel: String = "Til dato"
) : FeltDto()

/**
 * Nedtrekksliste med predefinerte alternativer.
 *
 * @property alternativer Liste over mulige valg
 */
data class SelectFeltDto(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = true,
    val alternativer: List<AlternativDto>
) : FeltDto()

/**
 * Land-velger (spesialisert nedtrekksliste for land).
 * Frontend rendrer dette med en landvelger-komponent.
 */
data class CountrySelectFeltDto(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = true
) : FeltDto()

/**
 * Liste-felt for repeterende elementer (f.eks. familiemedlemmer, virksomheter).
 *
 * @property leggTilLabel Label for "Legg til"-knappen
 * @property fjernLabel Label for "Fjern"-knappen
 * @property tomListeMelding Melding som vises når listen er tom
 * @property elementDefinisjon Definisjon av feltene i hvert liste-element
 */
data class ListeFeltDto(
    override val label: String,
    override val hjelpetekst: String? = null,
    override val pakrevd: Boolean = false,
    val leggTilLabel: String = "Legg til",
    val fjernLabel: String = "Fjern",
    val tomListeMelding: String? = null,
    val elementDefinisjon: Map<String, FeltDto>
) : FeltDto()
