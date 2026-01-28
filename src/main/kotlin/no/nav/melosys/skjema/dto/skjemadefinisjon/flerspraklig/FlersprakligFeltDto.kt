package no.nav.melosys.skjema.dto.skjemadefinisjon.flerspraklig

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.melosys.skjema.dto.skjemadefinisjon.*
import no.nav.melosys.skjema.service.skjemadefinisjon.Språk

/**
 * Sealed class for flerspråklige feltdefinisjoner.
 * Alle tekstfelter er av typen FlersprakligTekst.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = FlersprakligBooleanFeltDto::class, name = "BOOLEAN"),
    JsonSubTypes.Type(value = FlersprakligTextFeltDto::class, name = "TEXT"),
    JsonSubTypes.Type(value = FlersprakligTextareaFeltDto::class, name = "TEXTAREA"),
    JsonSubTypes.Type(value = FlersprakligDateFeltDto::class, name = "DATE"),
    JsonSubTypes.Type(value = FlersprakligPeriodeFeltDto::class, name = "PERIOD"),
    JsonSubTypes.Type(value = FlersprakligSelectFeltDto::class, name = "SELECT"),
    JsonSubTypes.Type(value = FlersprakligCountrySelectFeltDto::class, name = "COUNTRY_SELECT"),
    JsonSubTypes.Type(value = FlersprakligListeFeltDto::class, name = "LIST")
)
sealed class FlersprakligFeltDto {
    abstract val label: FlersprakligTekst
    abstract val hjelpetekst: FlersprakligTekst?
    abstract val pakrevd: Boolean

    /**
     * Transformerer til enkeltspråklig FeltDefinisjon.
     */
    abstract fun tilFeltDto(språk: Språk): FeltDefinisjon
}

data class FlersprakligBooleanFeltDto(
    override val label: FlersprakligTekst,
    override val hjelpetekst: FlersprakligTekst? = null,
    override val pakrevd: Boolean = true,
    val jaLabel: FlersprakligTekst,
    val neiLabel: FlersprakligTekst
) : FlersprakligFeltDto() {
    override fun tilFeltDto(språk: Språk) = BooleanFeltDefinisjon(
        label = label.hent(språk),
        hjelpetekst = hjelpetekst?.hent(språk),
        pakrevd = pakrevd,
        jaLabel = jaLabel.hent(språk),
        neiLabel = neiLabel.hent(språk)
    )
}

data class FlersprakligTextFeltDto(
    override val label: FlersprakligTekst,
    override val hjelpetekst: FlersprakligTekst? = null,
    override val pakrevd: Boolean = true
) : FlersprakligFeltDto() {
    override fun tilFeltDto(språk: Språk) = TextFeltDefinisjon(
        label = label.hent(språk),
        hjelpetekst = hjelpetekst?.hent(språk),
        pakrevd = pakrevd
    )
}

data class FlersprakligTextareaFeltDto(
    override val label: FlersprakligTekst,
    override val hjelpetekst: FlersprakligTekst? = null,
    override val pakrevd: Boolean = true,
    val maxLength: Int? = null
) : FlersprakligFeltDto() {
    override fun tilFeltDto(språk: Språk) = TextareaFeltDefinisjon(
        label = label.hent(språk),
        hjelpetekst = hjelpetekst?.hent(språk),
        pakrevd = pakrevd,
        maxLength = maxLength
    )
}

data class FlersprakligDateFeltDto(
    override val label: FlersprakligTekst,
    override val hjelpetekst: FlersprakligTekst? = null,
    override val pakrevd: Boolean = true
) : FlersprakligFeltDto() {
    override fun tilFeltDto(språk: Språk) = DateFeltDefinisjon(
        label = label.hent(språk),
        hjelpetekst = hjelpetekst?.hent(språk),
        pakrevd = pakrevd
    )
}

data class FlersprakligPeriodeFeltDto(
    override val label: FlersprakligTekst,
    override val hjelpetekst: FlersprakligTekst? = null,
    override val pakrevd: Boolean = true,
    val fraDatoLabel: FlersprakligTekst,
    val tilDatoLabel: FlersprakligTekst
) : FlersprakligFeltDto() {
    override fun tilFeltDto(språk: Språk) = PeriodeFeltDefinisjon(
        label = label.hent(språk),
        hjelpetekst = hjelpetekst?.hent(språk),
        pakrevd = pakrevd,
        fraDatoLabel = fraDatoLabel.hent(språk),
        tilDatoLabel = tilDatoLabel.hent(språk)
    )
}

data class FlersprakligAlternativDto(
    val verdi: String,
    val label: FlersprakligTekst,
    val beskrivelse: FlersprakligTekst? = null
) {
    fun tilAlternativDto(språk: Språk) = AlternativDefinisjon(
        verdi = verdi,
        label = label.hent(språk),
        beskrivelse = beskrivelse?.hent(språk)
    )
}

data class FlersprakligSelectFeltDto(
    override val label: FlersprakligTekst,
    override val hjelpetekst: FlersprakligTekst? = null,
    override val pakrevd: Boolean = true,
    val alternativer: List<FlersprakligAlternativDto>
) : FlersprakligFeltDto() {
    override fun tilFeltDto(språk: Språk) = SelectFeltDefinisjon(
        label = label.hent(språk),
        hjelpetekst = hjelpetekst?.hent(språk),
        pakrevd = pakrevd,
        alternativer = alternativer.map { it.tilAlternativDto(språk) }
    )
}

data class FlersprakligCountrySelectFeltDto(
    override val label: FlersprakligTekst,
    override val hjelpetekst: FlersprakligTekst? = null,
    override val pakrevd: Boolean = true
) : FlersprakligFeltDto() {
    override fun tilFeltDto(språk: Språk) = CountrySelectFeltDefinisjon(
        label = label.hent(språk),
        hjelpetekst = hjelpetekst?.hent(språk),
        pakrevd = pakrevd
    )
}

data class FlersprakligListeFeltDto(
    override val label: FlersprakligTekst,
    override val hjelpetekst: FlersprakligTekst? = null,
    override val pakrevd: Boolean = false,
    val leggTilLabel: FlersprakligTekst,
    val fjernLabel: FlersprakligTekst,
    val tomListeMelding: FlersprakligTekst? = null,
    val elementDefinisjon: Map<String, FlersprakligFeltDto>
) : FlersprakligFeltDto() {
    override fun tilFeltDto(språk: Språk) = ListeFeltDefinisjon(
        label = label.hent(språk),
        hjelpetekst = hjelpetekst?.hent(språk),
        pakrevd = pakrevd,
        leggTilLabel = leggTilLabel.hent(språk),
        fjernLabel = fjernLabel.hent(språk),
        tomListeMelding = tomListeMelding?.hent(språk),
        elementDefinisjon = elementDefinisjon.mapValues { it.value.tilFeltDto(språk) }
    )
}
