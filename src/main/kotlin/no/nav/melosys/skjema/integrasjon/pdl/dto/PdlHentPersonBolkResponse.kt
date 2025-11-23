package no.nav.melosys.skjema.integrasjon.pdl.dto

/**
 * Response fra PDL hentPersonBolk query
 */
data class PdlHentPersonBolkResponse(
    val hentPersonBolk: List<PdlPersonBolkEntry>
)
