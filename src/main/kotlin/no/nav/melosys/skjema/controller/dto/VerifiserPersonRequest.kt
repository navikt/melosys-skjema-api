package no.nav.melosys.skjema.controller.dto

import jakarta.validation.constraints.NotBlank
import no.nav.melosys.skjema.controller.validators.ErFodselsEllerDNummer

/**
 * Request for å verifisere en person uten fullmakt
 */
data class VerifiserPersonRequest(
    /**
     * Fødselsnummer eller d-nummer (11 siffer)
     */
    @field:NotBlank
    @field:ErFodselsEllerDNummer
    val fodselsnummer: String,

    /**
     * Etternavn for verifisering
     */
    @field:NotBlank
    val etternavn: String
)
