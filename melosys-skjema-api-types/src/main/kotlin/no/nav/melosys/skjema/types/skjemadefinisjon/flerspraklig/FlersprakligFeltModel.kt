package no.nav.melosys.skjema.types.skjemadefinisjon.flerspraklig

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.skjemadefinisjon.AlternativDefinisjonDto
import no.nav.melosys.skjema.types.skjemadefinisjon.BooleanFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.CountrySelectFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.DateFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.FeltDefinisjonDto
import no.nav.melosys.skjema.types.skjemadefinisjon.ListeFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.PeriodeFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.SelectFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.TextFeltDefinisjon
import no.nav.melosys.skjema.types.skjemadefinisjon.TextareaFeltDefinisjon

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
sealed class FlersprakligFeltModel {
    abstract val label: FlersprakligTekst
    abstract val hjelpetekst: FlersprakligTekst?
    abstract val pakrevd: Boolean

    /**
     * Transformerer til enkeltspråklig FeltDefinisjonDto.
     */
    abstract fun tilFeltDto(språk: Språk): FeltDefinisjonDto
}

data class FlersprakligBooleanFeltDto(
    override val label: FlersprakligTekst,
    override val hjelpetekst: FlersprakligTekst? = null,
    override val pakrevd: Boolean = true,
    val jaLabel: FlersprakligTekst,
    val neiLabel: FlersprakligTekst
) : FlersprakligFeltModel() {
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
) : FlersprakligFeltModel() {
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
) : FlersprakligFeltModel() {
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
) : FlersprakligFeltModel() {
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
) : FlersprakligFeltModel() {
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
    fun tilAlternativDto(språk: Språk) = AlternativDefinisjonDto(
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
) : FlersprakligFeltModel() {
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
) : FlersprakligFeltModel() {
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
    val elementDefinisjon: Map<String, FlersprakligFeltModel>
) : FlersprakligFeltModel() {
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
