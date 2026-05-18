package no.nav.melosys.skjema.types.utsendtarbeidstaker

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FamiliemedlemmerDto(
    val skalHaMedFamiliemedlemmer: Boolean,
)
