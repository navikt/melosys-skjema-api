package no.nav.melosys.skjema.dto

import java.util.UUID
import no.nav.melosys.skjema.entity.SkjemaStatus

data class OpprettSoknadMedKontekstResponse(
    val id: UUID,
    val status: SkjemaStatus
)
