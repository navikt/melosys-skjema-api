package no.nav.melosys.skjema.dto

data class RessursRegisterResponse(
    val links: Map<String, Any> = emptyMap(),
    val data: List<ResourceRegistryItem> = emptyList()
)

data class ResourceRegistryItem(
    val type: String,
    val value: String,
    val urn: String
)