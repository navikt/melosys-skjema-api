package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.dto.felles.LandKode

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaSkipDto(
    val navnPaVirksomhet: String,
    val navnPaSkip: String,
    val yrketTilArbeidstaker: String,
    val seilerI: Farvann,
    val flaggland: LandKode?,
    val territorialfarvannLand: LandKode?
)
