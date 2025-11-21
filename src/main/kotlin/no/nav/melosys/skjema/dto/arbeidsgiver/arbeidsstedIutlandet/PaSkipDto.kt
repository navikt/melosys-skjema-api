package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet.GyldigPaSkip

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigPaSkip
data class PaSkipDto(
    val navnPaSkip: String,
    val yrketTilArbeidstaker: String,
    val seilerI: Farvann,
    val flaggland: String?,
    val territorialfarvannLand: String?
)
