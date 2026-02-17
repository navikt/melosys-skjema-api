package no.nav.melosys.skjema.pdf

import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.types.arbeidsgiver.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.skjemadefinisjon.SkjemaDefinisjonDto

/**
 * Intern dataklasse for PDF-generering.
 * Inneholder all data som trengs for å generere PDF av et innsendt skjema.
 */
data class SkjemaPdfData(
    val skjemaId: UUID,
    val referanseId: String,
    val innsendtDato: Instant,
    val innsendtSprak: Språk,
    val arbeidstakerData: UtsendtArbeidstakerArbeidstakersSkjemaDataDto?,
    val arbeidsgiverData: UtsendtArbeidstakerArbeidsgiversSkjemaDataDto?,
    val definisjon: SkjemaDefinisjonDto
)
