package no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GraphQLResponse<T>(
    val data: T?,
    val errors: List<GraphQLError>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GraphQLError(
    val message: String,
    val extensions: Map<String, Any?>? = null
)