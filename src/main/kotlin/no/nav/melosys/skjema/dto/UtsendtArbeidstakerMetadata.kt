package no.nav.melosys.skjema.dto

import no.nav.melosys.skjema.domain.InnsendingMetadata

/**
 * Metadata for Utsendt Arbeidstaker skjema.
 * Lagres i skjema.metadata som JSONB.
 */
data class UtsendtArbeidstakerMetadata(
    val representasjonstype: Representasjonstype,
    val harFullmakt: Boolean,
    val radgiverfirma: RadgiverfirmaInfo? = null,
    val arbeidsgiverNavn: String? = null,
    val fullmektigFnr: String? = null,
    val innsending: InnsendingMetadata? = null
)

data class RadgiverfirmaInfo(
    val orgnr: String,
    val navn: String
)

enum class Representasjonstype {
    DEG_SELV,
    ARBEIDSGIVER,
    RADGIVER,
    ANNEN_PERSON
}
