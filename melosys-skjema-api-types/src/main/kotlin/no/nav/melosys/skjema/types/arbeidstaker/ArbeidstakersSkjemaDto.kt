package no.nav.melosys.skjema.types.arbeidstaker

import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.types.common.SkjemaStatus

data class ArbeidstakersSkjemaDto(
    val id: UUID,
    val fnr: String,
    val status: SkjemaStatus,
    val innsendtDato: Instant? = null,
    val erstatterSkjemaId: UUID? = null,
    val data: ArbeidstakersSkjemaDataDto = ArbeidstakersSkjemaDataDto()
)