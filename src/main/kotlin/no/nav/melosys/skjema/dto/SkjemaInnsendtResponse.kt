package no.nav.melosys.skjema.dto

import no.nav.melosys.skjema.entity.SkjemaStatus
import java.util.UUID

/**
 * Response ved innsending av skjema.
 * Inneholder referanseId som bruker kan referere til ved kontakt med NAV.
 */
data class SkjemaInnsendtResponse(
    val skjemaId: UUID,
    val referanseId: String,
    val status: SkjemaStatus
)
