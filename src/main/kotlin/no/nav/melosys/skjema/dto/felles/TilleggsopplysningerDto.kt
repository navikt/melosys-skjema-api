package no.nav.melosys.skjema.dto.felles

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.controller.validators.tilleggsopplysninger.GyldigTilleggsopplysninger

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigTilleggsopplysninger
data class TilleggsopplysningerDto(
    val harFlereOpplysningerTilSoknaden: Boolean,
    val tilleggsopplysningerTilSoknad: String?
)