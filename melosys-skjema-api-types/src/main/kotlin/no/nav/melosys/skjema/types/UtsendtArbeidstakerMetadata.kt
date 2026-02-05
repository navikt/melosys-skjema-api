package no.nav.melosys.skjema.types

/**
 * Metadata for Utsendt Arbeidstaker skjema.
 * Lagres i skjema.metadata som JSONB.
 *
 * Sealed class hierarki basert på representasjonstype:
 * - [DegSelvMetadata] - Arbeidstaker fyller ut selv
 * - [ArbeidsgiverMetadata] - Arbeidsgiver fyller ut (med eller uten fullmakt)
 * - [RadgiverMetadata] - Rådgiver fyller ut (med eller uten fullmakt)
 * - [AnnenPersonMetadata] - Annen person med fullmakt fyller ut
 *
 * Merk: Innsendingsstatus ligger på egne felt i Skjema-entiteten,
 * ikke her, siden det er felles for alle skjematyper.
 */
sealed class UtsendtArbeidstakerMetadata : SkjemaMetadata() {
    /** Hvilken del av skjemaet (arbeidstaker eller arbeidsgiver) */
    abstract val skjemadel: Skjemadel
    /** Navn på arbeidsgiver-organisasjonen */
    abstract val arbeidsgiverNavn: String
    /**
     * Juridisk enhet orgnr fra Enhetsregisteret.
     * Brukes for kobling av separate søknader (arbeidsgiver-del og arbeidstaker-del).
     * Ulike underenheter kan tilhøre samme juridiske enhet.
     */
    abstract val juridiskEnhetOrgnr: String
    /**
     * Referanse til koblet skjema-instans.
     * Når arbeidsgiver-del og arbeidstaker-del sendes separat, kobles de sammen
     * basert på fnr, juridisk enhet og overlappende perioder.
     */
    abstract val kobletSkjemaId: java.util.UUID?

    /** Representasjonstype for denne metadataen */
    abstract val representasjonstype: Representasjonstype

    /** Om innsender har fullmakt til å fylle ut på vegne av arbeidstaker */
    abstract val harFullmakt: Boolean

    /** Fødselsnummer til fullmektig (den som fyller ut på vegne av arbeidstaker) */
    abstract val fullmektigFnr: String?

    /** Informasjon om rådgiverfirmaet (kun relevant for RADGIVER) */
    open val radgiverfirma: RadgiverfirmaInfo? get() = null

    /** Oppretter en kopi med ny kobletSkjemaId */
    abstract fun medKobletSkjemaId(kobletSkjemaId: java.util.UUID?): UtsendtArbeidstakerMetadata
}

/**
 * Metadata når arbeidstaker fyller ut skjemaet selv.
 */
data class DegSelvMetadata(
    override val skjemadel: Skjemadel,
    override val arbeidsgiverNavn: String,
    override val juridiskEnhetOrgnr: String,
    override val kobletSkjemaId: java.util.UUID? = null
) : UtsendtArbeidstakerMetadata() {
    override val metadatatype: String = "UTSENDT_ARBEIDSTAKER_DEG_SELV"
    override val representasjonstype: Representasjonstype = Representasjonstype.DEG_SELV
    override val harFullmakt: Boolean = false
    override val fullmektigFnr: String? = null
    override fun medKobletSkjemaId(kobletSkjemaId: java.util.UUID?) = copy(kobletSkjemaId = kobletSkjemaId)
}

/**
 * Metadata når arbeidsgiver fyller ut skjemaet.
 * Kan være med eller uten fullmakt.
 */
data class ArbeidsgiverMetadata(
    override val skjemadel: Skjemadel,
    override val arbeidsgiverNavn: String,
    override val juridiskEnhetOrgnr: String,
    override val harFullmakt: Boolean,
    override val fullmektigFnr: String? = null,
    override val kobletSkjemaId: java.util.UUID? = null
) : UtsendtArbeidstakerMetadata() {
    override val metadatatype: String = "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER"
    override val representasjonstype: Representasjonstype = Representasjonstype.ARBEIDSGIVER
    override fun medKobletSkjemaId(kobletSkjemaId: java.util.UUID?) = copy(kobletSkjemaId = kobletSkjemaId)
}

/**
 * Metadata når rådgiver fyller ut skjemaet.
 * Inkluderer informasjon om rådgiverfirmaet.
 * Kan være med eller uten fullmakt.
 */
data class RadgiverMetadata(
    override val skjemadel: Skjemadel,
    override val arbeidsgiverNavn: String,
    override val juridiskEnhetOrgnr: String,
    override val harFullmakt: Boolean,
    override val fullmektigFnr: String? = null,
    override val kobletSkjemaId: java.util.UUID? = null,
    /** Informasjon om rådgiverfirmaet */
    override val radgiverfirma: RadgiverfirmaInfo? = null
) : UtsendtArbeidstakerMetadata() {
    override val metadatatype: String = "UTSENDT_ARBEIDSTAKER_RADGIVER"
    override val representasjonstype: Representasjonstype = Representasjonstype.RADGIVER
    override fun medKobletSkjemaId(kobletSkjemaId: java.util.UUID?) = copy(kobletSkjemaId = kobletSkjemaId)
}

/**
 * Metadata når annen person med fullmakt fyller ut skjemaet.
 * Fullmakt er alltid påkrevd for denne typen.
 */
data class AnnenPersonMetadata(
    override val skjemadel: Skjemadel,
    override val arbeidsgiverNavn: String,
    override val juridiskEnhetOrgnr: String,
    /** Fødselsnummer til fullmektig (påkrevd for annen person) */
    override val fullmektigFnr: String,
    override val kobletSkjemaId: java.util.UUID? = null
) : UtsendtArbeidstakerMetadata() {
    override val metadatatype: String = "UTSENDT_ARBEIDSTAKER_ANNEN_PERSON"
    override val representasjonstype: Representasjonstype = Representasjonstype.ANNEN_PERSON
    override val harFullmakt: Boolean = true
    override fun medKobletSkjemaId(kobletSkjemaId: java.util.UUID?) = copy(kobletSkjemaId = kobletSkjemaId)
}

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
