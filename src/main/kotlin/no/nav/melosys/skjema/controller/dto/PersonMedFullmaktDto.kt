package no.nav.melosys.skjema.controller.dto

import java.time.LocalDate

/**
 * DTO for person som bruker har fullmakt fra.
 * Brukes i arbeidstaker-velger på oversiktssiden.
 */
data class PersonMedFullmaktDto(
    /**
     * Fødselsnummer til fullmaktsgiver
     * NB: Skal maskeres i frontend ved visning til bruker
     */
    val fnr: String,

    /**
     * Fullt navn på fullmaktsgiver
     */
    val navn: String,

    /**
     * Fødselsdato
     */
    val fodselsdato: LocalDate
)
