package no.nav.melosys.skjema.types.m2m

import no.nav.melosys.skjema.types.arbeidsgiver.ArbeidsgiversSkjemaDto
import no.nav.melosys.skjema.types.arbeidstaker.ArbeidstakersSkjemaDto

data class UtsendtArbeidstakerM2MSkjemaData(
    val arbeidstakersDeler: List<ArbeidstakersSkjemaDto>,
    val arbeidsgiversDeler: List<ArbeidsgiversSkjemaDto>,
    val referanseId: String,
)
