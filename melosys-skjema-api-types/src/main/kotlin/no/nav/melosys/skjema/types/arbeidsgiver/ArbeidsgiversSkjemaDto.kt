package no.nav.melosys.skjema.types.arbeidsgiver

import no.nav.melosys.skjema.types.common.SkjemaStatus
import java.util.UUID

data class ArbeidsgiversSkjemaDto(
    val id: UUID,
    val orgnr: String,
    val status: SkjemaStatus,
    val data: ArbeidsgiversSkjemaDataDto = ArbeidsgiversSkjemaDataDto()
)