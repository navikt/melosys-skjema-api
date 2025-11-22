package no.nav.melosys.skjema.integrasjon.pdl.dto

data class PdlNavn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String
) {
    fun fulltNavn(): String {
        return if (mellomnavn != null) {
            "$fornavn $mellomnavn $etternavn"
        } else {
            "$fornavn $etternavn"
        }
    }
}
