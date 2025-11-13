package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidstakeren

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidstakerenArbeidsgiversDelDto(
    val fodselsnummer: String
)
