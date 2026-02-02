package no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidsstedIUtlandetDto(
    val arbeidsstedType: ArbeidsstedType,
    @field:Valid val paLand: PaLandDto? = null,
    val offshore: OffshoreDto? = null,
    @field:Valid val paSkip: PaSkipDto? = null,
    @field:Valid val omBordPaFly: OmBordPaFlyDto? = null
)
