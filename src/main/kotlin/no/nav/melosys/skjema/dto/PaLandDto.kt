package no.nav.melosys.skjema.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaLandDto(
    val fastEllerVekslendeArbeidssted: FastEllerVekslendeArbeidssted,
    val fastArbeidssted: PaLandFastArbeidsstedDto?,
    val beskrivelseVekslende: String?,
    val erHjemmekontor: Boolean
)
