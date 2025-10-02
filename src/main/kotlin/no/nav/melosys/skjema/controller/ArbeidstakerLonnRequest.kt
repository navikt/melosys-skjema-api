package no.nav.melosys.skjema.controller

data class ArbeidstakerLonnRequest(
    val arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden: Boolean,
    val virksomheterSomUtbetalerLonnOgNaturalytelser: VirksomheterSomUtbetalerLonnOgNaturalytelser?
)