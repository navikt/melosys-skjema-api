package no.nav.melosys.skjema.dto.arbeidstaker

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.dto.arbeidstaker.dineopplysninger.DineOpplysningerDto
import no.nav.melosys.skjema.dto.arbeidstaker.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelDto
import no.nav.melosys.skjema.dto.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import no.nav.melosys.skjema.dto.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.dto.felles.TilleggsopplysningerDto

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidstakersSkjemaDataDto(
    val arbeidstakeren: DineOpplysningerDto? = null,
    val utenlandsoppdraget: UtenlandsoppdragetArbeidstakersDelDto? = null,
    val arbeidssituasjon: ArbeidssituasjonDto? = null,
    val skatteforholdOgInntekt: SkatteforholdOgInntektDto? = null,
    val familiemedlemmer: FamiliemedlemmerDto? = null,
    val tilleggsopplysninger: TilleggsopplysningerDto? = null
)