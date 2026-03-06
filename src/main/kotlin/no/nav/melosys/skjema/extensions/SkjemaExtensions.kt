package no.nav.melosys.skjema.extensions

import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.types.SkjemaDto
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto

/**
 * Konverterer en Skjema-entitet til SkjemaDto basert på skjematype.
 */
fun Skjema.toDto(): SkjemaDto = when (this.type) {
    SkjemaType.UTSENDT_ARBEIDSTAKER -> this.toUtsendtArbeidstakerDto()
}

/**
 * Konverterer en Skjema-entitet til UtsendtArbeidstakerSkjemaDto.
 */
fun Skjema.toUtsendtArbeidstakerDto(): UtsendtArbeidstakerSkjemaDto {
    val metadata = this.metadata as UtsendtArbeidstakerMetadata
    return UtsendtArbeidstakerSkjemaDto(
        id = this.id ?: error("Skjema ID is null"),
        status = this.status,
        type = this.type,
        fnr = this.fnr,
        orgnr = this.orgnr,
        metadata = metadata,
        data = this.data as? UtsendtArbeidstakerSkjemaData ?: metadata.skjemadel.emptyData()
    )
}

private fun Skjemadel.emptyData(): UtsendtArbeidstakerSkjemaData = when (this) {
    Skjemadel.ARBEIDSTAKERS_DEL -> UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
    Skjemadel.ARBEIDSGIVERS_DEL -> UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
    Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL -> UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto()
}
