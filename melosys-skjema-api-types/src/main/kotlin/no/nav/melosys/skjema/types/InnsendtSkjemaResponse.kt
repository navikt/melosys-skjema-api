package no.nav.melosys.skjema.types

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.types.arbeidsgiver.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.skjemadefinisjon.SkjemaDefinisjonDto

/**
 * Response for en innsendt søknad med 
 * Brukes for å vise innsendt søknad med korrekte tekster fra innsendingstidspunkt.
 */
@Schema(description = "Innsendt søknad med skjemadefinisjon for korrekt visning")
data class InnsendtSkjemaResponse(
    @param:Schema(description = "Skjema-ID")
    val skjemaId: UUID,

    @param:Schema(description = "Referansenummer", example = "MEL-AB12CD")
    val referanseId: String,

    @param:Schema(description = "Tidspunkt for innsending")
    val innsendtDato: Instant,

    @param:Schema(description = "Språk som ble brukt ved innsending", example = "nb")
    val innsendtSprak: Språk,

    @param:Schema(description = "Versjon av skjemadefinisjon som ble brukt", example = "1")
    val skjemaDefinisjonVersjon: String,

    @param:Schema(description = "Arbeidstakers del av søknaden")
    val arbeidstakerData: UtsendtArbeidstakerArbeidstakersSkjemaDataDto?,

    @param:Schema(description = "Arbeidsgivers del av søknaden")
    val arbeidsgiverData: UtsendtArbeidstakerArbeidsgiversSkjemaDataDto?,

    @param:Schema(description = "Skjemadefinisjon for visning (basert på lagret versjon)")
    val definisjon: SkjemaDefinisjonDto
)
