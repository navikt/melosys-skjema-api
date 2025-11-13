package no.nav.melosys.skjema.dto.arbeidsgiver

import no.nav.melosys.skjema.entity.SkjemaStatus
import java.util.UUID

data class ArbeidsgiversSkjemaDto(
    val id: UUID,
    val orgnr: String,
    val status: SkjemaStatus,
    val data: ArbeidsgiversSkjemaDataDto = ArbeidsgiversSkjemaDataDto()
)