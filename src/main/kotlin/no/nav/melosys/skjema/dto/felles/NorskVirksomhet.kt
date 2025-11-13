package no.nav.melosys.skjema.dto.felles

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NorskVirksomhet(
    val organisasjonsnummer: String
)