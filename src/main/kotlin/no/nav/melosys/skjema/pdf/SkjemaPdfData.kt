package no.nav.melosys.skjema.pdf

import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.skjemadefinisjon.SkjemaDefinisjonDto

/**
 * Informasjon om arbeidsgiver og arbeidstaker for visning øverst i PDF.
 * Hentes fra skjema-metadata og PDL — ikke fra selve skjemadataen.
 */
data class AktørInfo(
    val arbeidsgiverNavn: String,
    val orgnr: String,
    val arbeidstakerNavn: String,
    val arbeidstakerFnr: String
)

/**
 * Intern dataklasse for PDF-generering.
 * Inneholder all data som trengs for å generere PDF av et innsendt skjema.
 */
data class SkjemaPdfData(
    val skjemaId: UUID,
    val referanseId: String,
    val innsendtDato: Instant,
    val innsendtSprak: Språk,
    val aktørInfo: AktørInfo,
    val skjemaData: UtsendtArbeidstakerSkjemaData,
    val kobletSkjemaData: UtsendtArbeidstakerSkjemaData?,
    val definisjon: SkjemaDefinisjonDto
)
