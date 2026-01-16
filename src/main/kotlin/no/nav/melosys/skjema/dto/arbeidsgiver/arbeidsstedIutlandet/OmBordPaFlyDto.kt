package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet.GyldigOmBordPaFly
import no.nav.melosys.skjema.dto.felles.LandKode

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigOmBordPaFly
data class OmBordPaFlyDto(
    val navnPaVirksomhet: String,
    val hjemmebaseLand: LandKode,
    val hjemmebaseNavn: String,
    val erVanligHjemmebase: Boolean,
    val vanligHjemmebaseLand: LandKode?,
    val vanligHjemmebaseNavn: String?
)
