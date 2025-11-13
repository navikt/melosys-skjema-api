package no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiveren

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidsgiverenDto(
    val organisasjonsnummer: String,
    val organisasjonNavn: String
)