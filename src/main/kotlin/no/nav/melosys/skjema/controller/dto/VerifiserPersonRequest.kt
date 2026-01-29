package no.nav.melosys.skjema.controller.dto

import jakarta.validation.constraints.NotBlank

/**
 * Request for å verifisere en person uten fullmakt
 */
data class VerifiserPersonRequest(
    /**
     * Fødselsnummer eller d-nummer (11 siffer)
     */
    @field:NotBlank
    val fodselsnummer: String,

    /**
     * Etternavn for verifisering
     */
    @field:NotBlank
    val etternavn: String
)
