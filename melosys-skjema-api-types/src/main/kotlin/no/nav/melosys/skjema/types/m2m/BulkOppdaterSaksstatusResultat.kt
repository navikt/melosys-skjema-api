package no.nav.melosys.skjema.types.m2m

import java.util.UUID

/**
 * Resultat av massesynk. [antallOppdatert] teller unike innsendinger som faktisk fikk ENDRET
 * saksstatus – allerede synkede rader røres ikke, så en gjentatt synk rapporterer 0. Kan være
 * høyere enn antall rader i requesten, siden alle innsendinger på samme saksnummer oppdateres.
 * [ukjenteSkjemaIder] er skjema-id-er uten tilhørende innsending.
 */
data class BulkOppdaterSaksstatusResultat(
    val antallOppdatert: Int,
    val ukjenteSkjemaIder: List<UUID>
)
