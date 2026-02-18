package no.nav.melosys.skjema.types.m2m

import java.time.LocalDateTime
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaDto

data class UtsendtArbeidstakerM2MSkjemaData(
    val skjemaer: List<UtsendtArbeidstakerSkjemaDto>,
    val referanseId: String,
    val innsendtTidspunkt: LocalDateTime,
    val innsenderFnr: String,
)
