package no.nav.melosys.skjema.types.kafka

import java.util.UUID

/**
 * Kafka-melding som sendes når et skjema er mottatt og journalført.
 * Sendes til topic: teammelosys.skjema.innsendt.v1-q2
 *
 * @param gruppeId stabil felles ID for alle relaterte deler av samme søknad (samme fnr +
 *   juridisk enhet + overlappende periode) — id-en til det tidligst opprettede skjemaet i gruppen.
 *   Konsumenten (melosys-api) bruker den til å serialisere behandlingen av relaterte deler per
 *   gruppe, uten å eksponere person-ID. Nullable for bakoverkompatibilitet med eldre meldinger.
 */
data class SkjemaMottattMelding(
    val skjemaId: UUID,
    val relaterteSkjemaIder: List<UUID> = emptyList(),
    val gruppeId: UUID? = null
)
