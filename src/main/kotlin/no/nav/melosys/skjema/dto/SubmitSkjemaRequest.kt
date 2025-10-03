package no.nav.melosys.skjema.dto

import java.time.Instant

data class SubmitSkjemaRequest(
    val bekreftetRiktighet: Boolean,
    val submittedAt: Instant
)