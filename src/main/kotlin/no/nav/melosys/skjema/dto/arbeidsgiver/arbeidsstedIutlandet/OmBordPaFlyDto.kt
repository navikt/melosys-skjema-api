package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OmBordPaFlyDto(
    val hjemmebaseLand: String,
    val hjemmebaseNavn: String,
    val erVanligHjemmebase: Boolean,
    val vanligHjemmebaseLand: String?,
    val vanligHjemmebaseNavn: String?
)
