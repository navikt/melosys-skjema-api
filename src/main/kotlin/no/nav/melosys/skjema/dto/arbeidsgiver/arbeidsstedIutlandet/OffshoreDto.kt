package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.dto.felles.LandKode

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffshoreDto(
    val navnPaInnretning: String,
    val typeInnretning: TypeInnretning,
    val sokkelLand: LandKode
)
