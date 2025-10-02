package no.nav.melosys.skjema.controller

import java.time.Instant

data class OppsummeringRequest(
    val bekreftetRiktighet: Boolean,
    val submittedAt: Instant
)