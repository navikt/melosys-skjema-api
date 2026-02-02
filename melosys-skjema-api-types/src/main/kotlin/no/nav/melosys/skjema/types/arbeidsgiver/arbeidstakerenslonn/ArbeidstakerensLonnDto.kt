package no.nav.melosys.skjema.types.arbeidsgiver.arbeidstakerenslonn

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid
import no.nav.melosys.skjema.types.felles.NorskeOgUtenlandskeVirksomheter

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidstakerensLonnDto(
    val arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden: Boolean,
    @field:Valid
    val virksomheterSomUtbetalerLonnOgNaturalytelser: NorskeOgUtenlandskeVirksomheter?
)