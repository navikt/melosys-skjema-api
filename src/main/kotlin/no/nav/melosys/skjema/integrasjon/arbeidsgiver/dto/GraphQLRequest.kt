package no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto

data class GraphQLRequest<T>(
    val query: String,
    val variables: T
)