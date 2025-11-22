package no.nav.melosys.skjema.integrasjon.felles.graphql

data class GraphQLResponse<T>(
    val data: T?,
    val errors: List<GraphQLError>?
)
