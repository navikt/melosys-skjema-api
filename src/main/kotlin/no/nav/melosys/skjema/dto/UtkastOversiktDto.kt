package no.nav.melosys.skjema.dto

import no.nav.melosys.skjema.entity.SkjemaStatus
import java.time.Instant
import java.util.UUID

/**
 * DTO for utkastoversikt.
 * Inneholder minimal informasjon for visning av påbegynte søknader.
 */
data class UtkastOversiktDto(
    val id: UUID,
    val arbeidsgiverNavn: String?,
    val arbeidsgiverOrgnr: String?,
    val arbeidstakerNavn: String?,
    val arbeidstakerFnrMaskert: String?, // Maskert fnr (f.eks. "01019*****")
    val opprettetDato: Instant,
    val sistEndretDato: Instant,
    val status: SkjemaStatus
)

/**
 * Response-objekt for liste av utkast.
 */
data class UtkastListeResponse(
    val utkast: List<UtkastOversiktDto>,
    val antall: Int
)
