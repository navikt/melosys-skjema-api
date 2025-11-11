package no.nav.melosys.skjema.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OffshoreDto(
    val navnPaInnretning: String,
    val typeInnretning: TypeInnretning,
    val sokkelLand: String
)
