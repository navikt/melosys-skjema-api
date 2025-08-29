package no.nav.melosys.skjema.dto

data class AltinnResponse(
    val hierarki: List<AltinnOrganization>,
    val orgNrTilTilganger: Map<String, List<String>>,
    val tilgangTilOrgNr: Map<String, List<String>>,
    val error: Boolean
)