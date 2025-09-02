package no.nav.melosys.skjema.integrasjon.altinn.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AltinnTilgangerRequest(
    val filter: Filter? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Filter(
    val altinn2Tilganger: Set<String>? = null,
    val altinn3Tilganger: Set<String>? = null,
    val inkluderSlettede: Boolean = false
)