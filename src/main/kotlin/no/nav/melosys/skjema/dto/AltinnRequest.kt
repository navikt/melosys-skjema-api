package no.nav.melosys.skjema.dto

data class AltinnRequest(
    val fnr: String,
    val filter: AltinnFilter
)