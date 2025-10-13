package no.nav.melosys.skjema.dto

data class ArbeidstakerensLonnDto(
    val arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden: Boolean,
    val virksomheterSomUtbetalerLonnOgNaturalytelser: NorskeOgUtenlandskeVirksomheter?
)