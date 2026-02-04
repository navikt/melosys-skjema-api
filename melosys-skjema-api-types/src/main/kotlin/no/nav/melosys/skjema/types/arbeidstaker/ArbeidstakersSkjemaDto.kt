package no.nav.melosys.skjema.types.arbeidstaker

import java.util.UUID
import no.nav.melosys.skjema.types.common.SkjemaStatus

data class ArbeidstakersSkjemaDto(
    val id: UUID,
    val fnr: String,
    val status: SkjemaStatus,
    val data: ArbeidstakersSkjemaDataDto = ArbeidstakersSkjemaDataDto()
)