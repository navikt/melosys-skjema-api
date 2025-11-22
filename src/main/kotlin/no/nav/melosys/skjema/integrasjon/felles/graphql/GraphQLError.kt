package no.nav.melosys.skjema.integrasjon.felles.graphql

data class GraphQLError(
    val message: String,
    val extensions: GraphQLErrorExtensions?
) {
    fun hasExtension(): Boolean = extensions != null
}

data class GraphQLErrorExtensions(
    val code: String?,
    val classification: String?
) {
    fun hasCode(code: String): Boolean = this.code == code
}
