package no.nav.melosys.skjema.dto.arbeidstaker

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.dto.arbeidstaker.arbeidstakeren.ArbeidstakerenDto
import no.nav.melosys.skjema.dto.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.dto.felles.TilleggsopplysningerDto

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidstakersSkjemaDataDto(
    val arbeidstakeren: ArbeidstakerenDto? = null,
    val skatteforholdOgInntekt: SkatteforholdOgInntektDto? = null,
    val familiemedlemmer: FamiliemedlemmerDto? = null,
    val tilleggsopplysninger: TilleggsopplysningerDto? = null
)