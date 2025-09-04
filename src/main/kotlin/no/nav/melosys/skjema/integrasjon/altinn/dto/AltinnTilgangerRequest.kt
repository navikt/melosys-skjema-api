package no.nav.melosys.skjema.integrasjon.altinn.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AltinnTilgangerRequest(
    val filter: AltinnFilter? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AltinnFilter(
    val altinn2Tilganger: Set<String> = emptySet(),
    val altinn3Tilganger: Set<String> = emptySet(),
    val inkluderSlettede: Boolean = false
)