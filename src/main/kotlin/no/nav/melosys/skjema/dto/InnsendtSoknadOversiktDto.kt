package no.nav.melosys.skjema.dto

import no.nav.melosys.skjema.entity.SkjemaStatus
import java.time.Instant
import java.util.UUID

/**
 * DTO for oversikt over innsendte søknader.
 * Inneholder informasjon som vises i tabellen på oversiktssiden.
 */
data class InnsendtSoknadOversiktDto(
    val id: UUID,
    val arbeidsgiverNavn: String?,
    val arbeidsgiverOrgnr: String?,
    val arbeidstakerNavn: String?,
    val arbeidstakerFnrMaskert: String?, // Maskert fnr (f.eks. "010190*****")
    val innsendtDato: Instant,
    val status: SkjemaStatus,
    val harPdf: Boolean = false // For fremtidig PDF-funksjonalitet
)

/**
 * Response-objekt for paginert liste av innsendte søknader.
 */
data class InnsendteSoknaderResponse(
    val soknader: List<InnsendtSoknadOversiktDto>,
    val totaltAntall: Int,
    val side: Int,
    val antallPerSide: Int
)
