package no.nav.melosys.skjema.extensions

import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.exception.SkjemaTypeMismatchException
import no.nav.melosys.skjema.types.SkjemaDto
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.felles.PeriodeDto
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
    val metadata = this.utsendtArbeidstakerMetadataOrThrow()
    return UtsendtArbeidstakerSkjemaDto(
        id = this.id ?: error("Skjema ID is null"),
        status = this.status,
        type = this.type,
        fnr = this.fnr,
        orgnr = this.orgnr,
        opprettetDato = this.opprettetDato.toOsloLocalDateTime(),
        endretDato = this.endretDato.toOsloLocalDateTime(),
        metadata = metadata,
        data = this.utsendtArbeidstakerSkjemaDataOrEmpty()
    )
}

private fun Skjemadel.emptyData(): UtsendtArbeidstakerSkjemaData = when (this) {
    Skjemadel.ARBEIDSTAKERS_DEL -> UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
    Skjemadel.ARBEIDSGIVERS_DEL -> UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
    Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL -> UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto()
}

fun Skjema.utsendtArbeidstakerMetadataOrThrow(): UtsendtArbeidstakerMetadata =
    this.metadata as? UtsendtArbeidstakerMetadata
        ?: throw SkjemaTypeMismatchException("Forventet ${UtsendtArbeidstakerMetadata::class.simpleName} for skjema $id, men var ${metadata::class.simpleName}")

fun Skjema.utsendtArbeidstakerSkjemaDataOrThrow(): UtsendtArbeidstakerSkjemaData =
    this.data as? UtsendtArbeidstakerSkjemaData
        ?: throw SkjemaTypeMismatchException("Forventet ${UtsendtArbeidstakerSkjemaData::class.simpleName} for skjema $id, men var ${data?.let { it::class.simpleName }}")

fun Skjema.utsendtArbeidstakerSkjemaDataOrEmpty(): UtsendtArbeidstakerSkjemaData =
    this.data as? UtsendtArbeidstakerSkjemaData
        ?: this.utsendtArbeidstakerMetadataOrThrow().skjemadel.emptyData()

fun Skjema.utsendelsePeriode(): PeriodeDto? =
    (this.data as? UtsendtArbeidstakerSkjemaData)?.utsendingsperiodeOgLand?.utsendelsePeriode
