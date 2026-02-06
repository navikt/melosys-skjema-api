package no.nav.melosys.skjema.types

import java.util.UUID
import no.nav.melosys.skjema.types.common.SkjemaStatus

data class UtsendtArbeidstakerSkjemaDto(
    override val id: UUID,
    override val status: SkjemaStatus,
    override val type: SkjemaType = SkjemaType.UTSENDT_ARBEIDSTAKER,
    override val fnr: String,
    override val orgnr: String,
    override val metadata: UtsendtArbeidstakerMetadata,
    override val data: UtsendtArbeidstakerSkjemaData
) : SkjemaDto
