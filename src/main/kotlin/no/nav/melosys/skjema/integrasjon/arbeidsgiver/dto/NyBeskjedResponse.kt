package no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NyBeskjedResponse(
    val nyBeskjed: BeskjedResult?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BeskjedResult(
    val __typename: String,
    val id: String? = null,
    val feilmelding: String? = null
)