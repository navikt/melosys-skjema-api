package no.nav.melosys.skjema.integrasjon.altinn.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AltinnTilgangerResponse(
    val isError: Boolean = false,
    val hierarki: List<AltinnTilgang> = emptyList(),
    val orgNrTilTilganger: Map<String, Set<String>> = emptyMap(),
    val tilgangTilOrgNr: Map<String, Set<String>> = emptyMap()
)