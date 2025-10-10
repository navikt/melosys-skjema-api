package no.nav.melosys.skjema.dto

data class ArbeidstakersSkjemaDataDto(
    val arbeidstakeren: ArbeidstakerenDto? = null,
    val skatteforholdOgInntekt: SkatteforholdOgInntektDto? = null
)