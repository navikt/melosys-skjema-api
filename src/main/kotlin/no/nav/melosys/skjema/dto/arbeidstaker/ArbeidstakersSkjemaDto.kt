package no.nav.melosys.skjema.dto.arbeidstaker

import no.nav.melosys.skjema.entity.SkjemaStatus
import java.util.UUID

data class ArbeidstakersSkjemaDto(
    val id: UUID,
    val fnr: String,
    val status: SkjemaStatus,
    val data: ArbeidstakersSkjemaDataDto = ArbeidstakersSkjemaDataDto()
)