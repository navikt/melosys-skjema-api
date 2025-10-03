package no.nav.melosys.skjema.dto

data class ArbeidstakerLonnRequest(
    val arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden: Boolean,
    val virksomheterSomUtbetalerLonnOgNaturalytelser: VirksomheterSomUtbetalerLonnOgNaturalytelser?
)