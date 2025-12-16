package no.nav.melosys.skjema.kafka

import java.util.UUID

/**
 * Kafka-melding som sendes når et skjema er mottatt og journalført.
 * Sendes til topic: teammelosys.skjema.innsendt.v1-q2
 */
data class SkjemaMottattMelding(
    val skjemaId: UUID
)
