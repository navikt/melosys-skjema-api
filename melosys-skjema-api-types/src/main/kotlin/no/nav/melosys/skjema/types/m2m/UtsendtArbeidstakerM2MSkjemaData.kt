package no.nav.melosys.skjema.types.m2m

import no.nav.melosys.skjema.types.arbeidsgiver.ArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.ArbeidstakersSkjemaDataDto

data class UtsendtArbeidstakerM2MSkjemaData(
    val arbeidstakersDel: ArbeidstakersSkjemaDataDto?,
    val arbeidsgiversDel: ArbeidsgiversSkjemaDataDto?,
    val referanseId: String,
)
