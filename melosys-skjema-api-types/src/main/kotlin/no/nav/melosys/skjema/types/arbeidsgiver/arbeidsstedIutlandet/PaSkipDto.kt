package no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.types.felles.LandKode

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaSkipDto(
    val navnPaVirksomhet: String,
    val navnPaSkip: String,
    val yrketTilArbeidstaker: String,
    val seilerI: Farvann,
    val flaggland: LandKode?,
    val territorialfarvannLand: LandKode?
)
