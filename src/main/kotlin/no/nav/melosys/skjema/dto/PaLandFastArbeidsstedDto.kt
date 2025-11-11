package no.nav.melosys.skjema.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PaLandFastArbeidsstedDto(
    val vegadresse: String,
    val nummer: String,
    val postkode: String,
    val bySted: String
)
