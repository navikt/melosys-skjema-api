package no.nav.melosys.skjema.integrasjon.felles.graphql

data class GraphQLRequest(
    val query: String,
    val variables: Map<String, Any>
)
