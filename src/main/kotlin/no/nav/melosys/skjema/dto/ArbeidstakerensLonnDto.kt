package no.nav.melosys.skjema.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidstakerensLonnDto(
    val arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden: Boolean,
    val virksomheterSomUtbetalerLonnOgNaturalytelser: NorskeOgUtenlandskeVirksomheter?
)