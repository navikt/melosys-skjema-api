package no.nav.melosys.skjema.types.m2m

import java.time.LocalDateTime
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.vedlegg.VedleggDto

data class UtsendtArbeidstakerSkjemaM2MDto(
    val skjema: UtsendtArbeidstakerSkjemaDto,
    val kobletSkjema: UtsendtArbeidstakerSkjemaDto?,
    val tidligereInnsendteSkjema: List<UtsendtArbeidstakerSkjemaDto>,
    val referanseId: String,
    val innsendtTidspunkt: LocalDateTime,
    val innsenderFnr: String,
    val vedlegg: List<VedleggDto> = emptyList(),
)
