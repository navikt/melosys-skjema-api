package no.nav.melosys.skjema.dto.m2m

import no.nav.melosys.skjema.dto.arbeidsgiver.ArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.dto.arbeidstaker.ArbeidstakersSkjemaDataDto

data class UtsendtArbeidstakerM2MSkjemaData(
    val arbeidstakersDel: ArbeidstakersSkjemaDataDto?,
    val arbeidsgiversDel: ArbeidsgiversSkjemaDataDto?,
    val referanseId: String,
    val journaposteringId: String = "Her skal det ligge en journapostering ID",
)
