package no.nav.melosys.skjema.event

import java.util.UUID

/**
 * Event som publiseres når en ny innsending er opprettet og klar for prosessering.
 * Brukes for å starte async prosessering ETTER at transaksjonen er committed.
 */
data class InnsendingOpprettetEvent(val skjemaId: UUID)
