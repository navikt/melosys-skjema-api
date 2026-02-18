package no.nav.melosys.skjema.types.m2m

import java.time.LocalDateTime
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaDto

data class UtsendtArbeidstakerSkjemaM2MDto(
    val skjema: UtsendtArbeidstakerSkjemaDto,
    val kobletSkjema: UtsendtArbeidstakerSkjemaDto?,
    val tidligereInnsendteSkjema: List<UtsendtArbeidstakerSkjemaDto>,
    val referanseId: String,
    val innsendtTidspunkt: LocalDateTime,
    val innsenderFnr: String,
)
