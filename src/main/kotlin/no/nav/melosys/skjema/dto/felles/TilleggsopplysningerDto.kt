package no.nav.melosys.skjema.dto.felles

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TilleggsopplysningerDto(
    val harFlereOpplysningerTilSoknaden: Boolean,
    val tilleggsopplysningerTilSoknad: String?
)