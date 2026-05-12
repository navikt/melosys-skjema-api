package no.nav.melosys.skjema.integrasjon.pdl.dto

data class PdlFoedselsdato(
    val foedselsdato: String,
    val metadata: PdlMetadata = PdlMetadata()
)
