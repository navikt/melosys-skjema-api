package no.nav.melosys.skjema.dto

data class RepresentasjonerResponse(
    val fnr: String,
    val orgnr: List<String>
)