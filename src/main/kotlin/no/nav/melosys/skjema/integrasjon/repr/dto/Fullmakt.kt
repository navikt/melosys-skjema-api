package no.nav.melosys.skjema.integrasjon.repr.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Fullmakt(
    val fullmaktsgiver: String,
    val fullmektig: String,
    val leserettigheter: List<String>,
    val skriverettigheter: List<String>
)
