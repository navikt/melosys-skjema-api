package no.nav.melosys.skjema.types

import java.util.UUID
import no.nav.melosys.skjema.types.common.SkjemaStatus

/**
 * Response ved innsending av skjema.
 * Inneholder referanseId som bruker kan referere til ved kontakt med NAV.
 */
data class SkjemaInnsendtKvittering(
    val skjemaId: UUID,
    val referanseId: String,
    val status: SkjemaStatus
)
