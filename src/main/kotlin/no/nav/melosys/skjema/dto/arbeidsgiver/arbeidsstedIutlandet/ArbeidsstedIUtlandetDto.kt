package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet.GyldigArbeidsstedIUtlandet

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigArbeidsstedIUtlandet
data class ArbeidsstedIUtlandetDto(
    val arbeidsstedType: ArbeidsstedType,
    val paLand: PaLandDto? = null,
    val offshore: OffshoreDto? = null,
    val paSkip: PaSkipDto? = null,
    val omBordPaFly: OmBordPaFlyDto? = null
)
