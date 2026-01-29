package no.nav.melosys.skjema.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.melosys.skjema.dto.arbeidsgiver.ArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.dto.arbeidstaker.ArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.dto.skjemadefinisjon.SkjemaDefinisjonDto
import no.nav.melosys.skjema.service.skjemadefinisjon.Språk
import java.time.Instant
import java.util.UUID

/**
 * Response for en innsendt søknad med skjemadefinisjon.
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
    val arbeidstakerData: ArbeidstakersSkjemaDataDto?,

    @param:Schema(description = "Arbeidsgivers del av søknaden")
    val arbeidsgiverData: ArbeidsgiversSkjemaDataDto?,

    @param:Schema(description = "Skjemadefinisjon for visning (basert på lagret versjon)")
    val definisjon: SkjemaDefinisjonDto
)
