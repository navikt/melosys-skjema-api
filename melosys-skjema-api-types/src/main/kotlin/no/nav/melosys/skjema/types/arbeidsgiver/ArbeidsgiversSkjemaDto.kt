package no.nav.melosys.skjema.types.arbeidsgiver

import java.util.UUID
import no.nav.melosys.skjema.types.common.SkjemaStatus

data class ArbeidsgiversSkjemaDto(
    val id: UUID,
    val orgnr: String,
    val status: SkjemaStatus,
    val data: ArbeidsgiversSkjemaDataDto = ArbeidsgiversSkjemaDataDto()
)