package no.nav.melosys.skjema.types

import java.util.UUID
import no.nav.melosys.skjema.types.common.SkjemaStatus

data class OpprettSoknadMedKontekstResponse(
    val id: UUID,
    val status: SkjemaStatus
)
