package no.nav.melosys.skjema.controller.dto

data class ErrorResponse(
    val message: String,
    val errors: Map<String, Any>? = null
)
