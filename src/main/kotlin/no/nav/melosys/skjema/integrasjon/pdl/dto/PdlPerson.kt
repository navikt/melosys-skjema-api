package no.nav.melosys.skjema.integrasjon.pdl.dto

data class PdlPerson(
    val navn: List<PdlNavn>,
    val foedselsdato: List<PdlFoedselsdato>
)
