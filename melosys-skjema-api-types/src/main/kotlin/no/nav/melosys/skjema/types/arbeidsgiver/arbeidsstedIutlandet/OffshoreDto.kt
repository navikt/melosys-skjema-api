package no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.types.felles.LandKode

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffshoreDto(
    val navnPaVirksomhet: String,
    val navnPaInnretning: String,
    val typeInnretning: TypeInnretning,
    val sokkelLand: LandKode
)
