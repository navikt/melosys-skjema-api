package no.nav.melosys.skjema.dto

data class AltinnOrganization(
    val orgnr: String,
    val erSlettet: Boolean,
    val altinn3Tilganger: List<String>,
    val altinn2Tilganger: List<String>,
    val underenheter: List<AltinnOrganization>,
    val navn: String,
    val organisasjonsform: String
)