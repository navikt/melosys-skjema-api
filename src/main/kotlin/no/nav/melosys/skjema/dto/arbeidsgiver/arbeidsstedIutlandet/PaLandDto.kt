package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet.GyldigPaLand

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigPaLand
data class PaLandDto(
    val navnPaVirksomhet: String,
    val fastEllerVekslendeArbeidssted: FastEllerVekslendeArbeidssted,
    val fastArbeidssted: PaLandFastArbeidsstedDto?,
    val beskrivelseVekslende: String?,
    val erHjemmekontor: Boolean
)
