package no.nav.melosys.skjema.types

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
    /**
     * Juridisk enhet orgnr fra Enhetsregisteret.
     * Brukes for kobling av separate søknader (arbeidsgiver-del og arbeidstaker-del).
     * Ulike underenheter kan tilhøre samme juridiske enhet.
     */
    val juridiskEnhetOrgnr: String? = null,
    /**
     * Referanse til koblet skjema-instans.
     * Når arbeidsgiver-del og arbeidstaker-del sendes separat, kobles de sammen
     * basert på fnr, juridisk enhet og overlappende perioder.
     */
    val kobletSkjemaId: java.util.UUID? = null
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
