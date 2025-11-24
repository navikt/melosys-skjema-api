package no.nav.melosys.skjema.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

/**
 * Request for å hente innsendte søknader med paginering, søk og sortering.
 *
 * Sikkerhet: Dette endepunktet bruker POST i stedet for GET for å unngå at
 * søkeord og andre potensielt sensitive parametre logges i access logs.
 */
data class HentInnsendteSoknaderRequest(
    /** Sidenummer (1-indeksert) */
    @field:NotNull
    @field:Min(1)
    val side: Int = 1,

    /** Antall resultater per side */
    @field:NotNull
    @field:Min(1)
    @field:Max(100)
    val antall: Int = 10,

    /** Fritekst-søk (søker i arbeidsgiver navn/orgnr, arbeidstaker navn) */
    val sok: String? = null,

    /** Felt å sortere på */
    val sortering: SorteringsFelt? = null,

    /** Sorteringsretning */
    val retning: Sorteringsretning? = null,

    /** Representasjonstype for filtrering */
    @field:NotNull
    val representasjonstype: Representasjonstype,

    /** Rådgiverfirma orgnr (kun påkrevd for RADGIVER) */
    val radgiverfirmaOrgnr: String? = null
)

enum class SorteringsFelt {
    ARBEIDSGIVER,
    ARBEIDSTAKER,
    INNSENDT_DATO,
    STATUS
}

enum class Sorteringsretning {
    ASC,
    DESC
}
