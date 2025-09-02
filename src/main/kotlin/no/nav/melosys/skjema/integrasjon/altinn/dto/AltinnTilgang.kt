package no.nav.melosys.skjema.integrasjon.altinn.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class AltinnTilgang(
    val orgnr: String,
    val navn: String,
    val organisasjonsform: String,
    val altinn2Tilganger: Set<String> = emptySet(),
    val altinn3Tilganger: Set<String> = emptySet(),
    val underenheter: List<AltinnTilgang> = emptyList(),
    val erSlettet: Boolean = false
)