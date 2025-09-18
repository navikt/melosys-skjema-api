package no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto

data class GraphQLRequest(
    val query: String,
    val variables: Map<String, Any?> = emptyMap()
)