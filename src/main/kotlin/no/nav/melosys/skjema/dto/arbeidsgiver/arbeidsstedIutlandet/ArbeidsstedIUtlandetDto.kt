package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid
import no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet.GyldigArbeidsstedIUtlandet

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigArbeidsstedIUtlandet
data class ArbeidsstedIUtlandetDto(
    val arbeidsstedType: ArbeidsstedType,
    @field:Valid val paLand: PaLandDto? = null,
    val offshore: OffshoreDto? = null,
    @field:Valid val paSkip: PaSkipDto? = null,
    @field:Valid val omBordPaFly: OmBordPaFlyDto? = null
)
