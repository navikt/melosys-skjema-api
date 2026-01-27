package no.nav.melosys.skjema.dto

/**
 * Metadata for Utsendt Arbeidstaker skjema.
 * Lagres i skjema.metadata som JSONB.
 *
 * Merk: Innsendingsstatus ligger på egne felt i Skjema-entiteten,
 * ikke her, siden det er felles for alle skjematyper.
 */
data class UtsendtArbeidstakerMetadata(
    val representasjonstype: Representasjonstype,
    val harFullmakt: Boolean,
    val skjemadel: Skjemadel,
    val radgiverfirma: RadgiverfirmaInfo? = null,
    val arbeidsgiverNavn: String? = null,
    val fullmektigFnr: String? = null,
    /** Versjon av skjemadefinisjon som ble brukt ved innsending */
    val skjemaDefinisjonVersjon: String,
    /** Språk som ble brukt ved innsending */
    val innsendtSprak: String
)

data class RadgiverfirmaInfo(
    val orgnr: String,
    val navn: String
)

enum class Representasjonstype {
    DEG_SELV,
    ARBEIDSGIVER,
    RADGIVER,
    ANNEN_PERSON
}

enum class Skjemadel {
    ARBEIDSTAKERS_DEL,
    ARBEIDSGIVERS_DEL
}
