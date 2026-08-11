package no.nav.melosys.skjema.types.m2m

import java.util.UUID

/**
 * Resultat av massesynk. [antallOppdatert] teller innsendinger som faktisk fikk ENDRET
 * saksstatus – allerede synkede rader røres ikke, så en gjentatt synk rapporterer 0.
 * Hver rad oppdaterer kun sin egen innsending (per skjema-id).
 * [ukjenteSkjemaIder] er skjema-id-er uten tilhørende innsending.
 * [konfliktSkjemaIder] er rader der innsendingen allerede har et ANNET saksnummer enn
 * requesten oppga (saksnummer er immutabelt) – raden hoppes over uten å feile batchen.
 * Feltet har default tom liste slik at eldre konsumenter deserialiserer trygt.
 */
data class BulkOppdaterSaksstatusResultat(
    val antallOppdatert: Int,
    val ukjenteSkjemaIder: List<UUID>,
    val konfliktSkjemaIder: List<UUID> = emptyList()
)
