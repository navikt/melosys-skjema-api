package no.nav.melosys.skjema.types.kafka

import java.util.UUID
import no.nav.melosys.skjema.types.SkjemaType

/**
 * Kafka-melding som sendes når et skjema er mottatt og journalført.
 * Sendes til topic: teammelosys.skjema.innsendt.v1-q2
 */
data class SkjemaMottattMelding(
    val skjemaId: UUID
)