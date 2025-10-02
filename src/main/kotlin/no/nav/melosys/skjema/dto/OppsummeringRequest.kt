package no.nav.melosys.skjema.dto

import java.time.Instant

data class OppsummeringRequest(
    val bekreftetRiktighet: Boolean,
    val submittedAt: Instant
)