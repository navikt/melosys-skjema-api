package no.nav.melosys.skjema.integrasjon.repr.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class KanRepresenteresAvResponse(
    val fullmakter: List<Fullmakt> = emptyList()
)
