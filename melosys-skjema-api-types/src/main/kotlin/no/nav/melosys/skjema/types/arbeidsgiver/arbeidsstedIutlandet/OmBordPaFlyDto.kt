package no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.types.felles.LandKode

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OmBordPaFlyDto(
    val navnPaVirksomhet: String,
    val hjemmebaseLand: LandKode,
    val hjemmebaseNavn: String,
    val erVanligHjemmebase: Boolean,
    val vanligHjemmebaseLand: LandKode?,
    val vanligHjemmebaseNavn: String?
)
