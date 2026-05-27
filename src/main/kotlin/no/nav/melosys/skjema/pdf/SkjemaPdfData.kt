package no.nav.melosys.skjema.pdf

import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.skjemadefinisjon.SkjemaDefinisjonDto
import no.nav.melosys.skjema.types.vedlegg.VedleggDto

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
 * Informasjon om fullmektig som har sendt inn søknad på vegne av arbeidstaker.
 */
data class FullmektigInfo(
    val navn: String,
    val fnr: String
)

/**
 * Informasjon om rådgiverfirma som representerer arbeidsgiver,
 * og personen hos rådgiverfirmaet med delegert tilgang.
 */
data class RadgiverInfo(
    val firmaNavn: String,
    val firmaOrgnr: String,
    val personNavn: String,
    val personFnr: String
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
    val fullmektigInfo: FullmektigInfo? = null,
    val radgiverInfo: RadgiverInfo? = null,
    val skjemaData: UtsendtArbeidstakerSkjemaData,
    val kobletSkjemaData: UtsendtArbeidstakerSkjemaData?,
    val vedlegg: List<VedleggDto>,
    val kobletVedlegg: List<VedleggDto> = emptyList(),
    val definisjon: SkjemaDefinisjonDto
)
