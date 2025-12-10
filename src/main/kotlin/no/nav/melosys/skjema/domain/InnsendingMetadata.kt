package no.nav.melosys.skjema.domain

import java.time.Instant

/**
 * Metadata for sporing av asynkron prosessering av innsendte søknader.
 *
 * Lagres som del av skjema.metadata (JSONB).
 */
data class InnsendingMetadata(
    val status: InnsendingStatus,
    val journalpostId: String? = null,
    val referanseId: String? = null,
    val feilmelding: String? = null,
    val antallForsok: Int = 0,
    val sisteForsoek: Instant? = null
)
