package no.nav.melosys.skjema.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidstakersSkjemaDataDto(
    val arbeidstakeren: ArbeidstakerenDto? = null,
    val skatteforholdOgInntekt: SkatteforholdOgInntektDto? = null,
    val familiemedlemmer: FamiliemedlemmerDto? = null,
    val tilleggsopplysninger: TilleggsopplysningerDto? = null
)