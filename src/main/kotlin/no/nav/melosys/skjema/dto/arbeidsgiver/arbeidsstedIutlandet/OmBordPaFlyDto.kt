package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet.GyldigOmBordPaFly

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigOmBordPaFly
data class OmBordPaFlyDto(
    val hjemmebaseLand: String,
    val hjemmebaseNavn: String,
    val erVanligHjemmebase: Boolean,
    val vanligHjemmebaseLand: String?,
    val vanligHjemmebaseNavn: String?
)
