package no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto

import java.util.UUID

data class BeskjedRequest(
    val virksomhetsnummer: String,
    val tekst: String,
    val lenke: String,
    val eksternId: String = UUID.randomUUID().toString(),
)