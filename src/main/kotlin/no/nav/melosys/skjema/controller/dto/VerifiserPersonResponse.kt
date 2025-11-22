package no.nav.melosys.skjema.controller.dto

import java.time.LocalDate

/**
 * Response fra person-verifisering
 */
data class VerifiserPersonResponse(
    /**
     * Fullt navn på personen
     */
    val navn: String,

    /**
     * Fødselsdato
     */
    val fodselsdato: LocalDate
)
