package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidsstedIUtlandetDto(
    val arbeidsstedType: ArbeidsstedType,
    val paLand: PaLandDto? = null,
    val offshore: OffshoreDto? = null,
    val paSkip: PaSkipDto? = null,
    val omBordPaFly: OmBordPaFlyDto? = null
)
