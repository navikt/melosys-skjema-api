package no.nav.melosys.skjema.types

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.skjemadefinisjon.SkjemaDefinisjonDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData

/**
 * Response for en innsendt søknad med skjemadefinisjon.
 * Brukes for å vise innsendt søknad med korrekte tekster fra innsendingstidspunkt.
 */
@Schema(description = "Innsendt søknad med skjemadefinisjon for korrekt visning")
data class InnsendtSkjemaResponse(
    @param:Schema(description = "Skjema-ID")
    val skjemaId: UUID,

    @param:Schema(description = "Referansenummer", example = "AB12CD")
    val referanseId: String,

    @param:Schema(description = "Tidspunkt for innsending")
    val innsendtDato: Instant,

    @param:Schema(description = "Språk som ble brukt ved innsending", example = "nb")
    val innsendtSprak: Språk,

    @param:Schema(description = "Versjon av skjemadefinisjon som ble brukt", example = "1")
    val skjemaDefinisjonVersjon: String,

    @param:Schema(description = "Skjemadata (polymorfisk — bruk 'type'-feltet for å avgjøre variant)")
    val skjemaData: UtsendtArbeidstakerSkjemaData,

    @param:Schema(description = "Skjemadefinisjon for visning (basert på lagret versjon)")
    val definisjon: SkjemaDefinisjonDto,

    @param:Schema(description = "Indikerer om fullmakt er aktiv. Arbeidstaker-data er strippet når false.")
    val fullmaktAktiv: Boolean = true
)
