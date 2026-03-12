package no.nav.melosys.skjema.types.utsendtarbeidstaker

import java.util.UUID
import no.nav.melosys.skjema.types.common.SkjemaStatus

data class OpprettUtsendtArbeidstakerSoknadResponse(
    val id: UUID,
    val status: SkjemaStatus
)
