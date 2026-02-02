package no.nav.melosys.skjema.types.arbeidstaker

import no.nav.melosys.skjema.types.common.SkjemaStatus
import java.util.UUID

data class ArbeidstakersSkjemaDto(
    val id: UUID,
    val fnr: String,
    val status: SkjemaStatus,
    val data: ArbeidstakersSkjemaDataDto = ArbeidstakersSkjemaDataDto()
)