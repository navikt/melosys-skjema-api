package no.nav.melosys.skjema.types.m2m

import no.nav.melosys.skjema.types.arbeidsgiver.ArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.ArbeidstakersSkjemaDataDto

data class UtsendtArbeidstakerM2MSkjemaData(
    val arbeidstakersDeler: List<ArbeidstakersSkjemaDataDto>,
    val arbeidsgiversDeler: List<ArbeidsgiversSkjemaDataDto>,
    val referanseId: String,
)
