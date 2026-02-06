package no.nav.melosys.skjema.types

/**
 * Metadata for Utsendt Arbeidstaker skjema.
 * Lagres i skjema.metadata som JSONB.
 *
 * Sealed class hierarki basert på representasjonstype:
 * - [DegSelvMetadata] - Arbeidstaker fyller ut selv
 * - [ArbeidsgiverMetadata] - Arbeidsgiver fyller ut uten fullmakt
 * - [ArbeidsgiverMedFullmaktMetadata] - Arbeidsgiver fyller ut med fullmakt
 * - [RadgiverMetadata] - Rådgiver fyller ut uten fullmakt
 * - [RadgiverMedFullmaktMetadata] - Rådgiver fyller ut med fullmakt
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
     * Referanse til koblet skjema-instans (motpart).
     * Når arbeidsgiver-del og arbeidstaker-del sendes separat, kobles de sammen
     * basert på fnr, juridisk enhet og overlappende perioder.
     */
    abstract val kobletSkjemaId: java.util.UUID?
    /**
     * Referanse til forrige versjon av samme skjemadel.
     * Settes når bruker sender inn samme del på nytt (ny versjon av samme søknad).
     */
    abstract val erstatterSkjemaId: java.util.UUID?

    /** Representasjonstype for denne metadataen */
    abstract val representasjonstype: Representasjonstype

    /** Oppretter en kopi med ny kobletSkjemaId */
    abstract fun medKobletSkjemaId(kobletSkjemaId: java.util.UUID?): UtsendtArbeidstakerMetadata

    /** Oppretter en kopi med ny erstatterSkjemaId */
    abstract fun medErstatterSkjemaId(erstatterSkjemaId: java.util.UUID?): UtsendtArbeidstakerMetadata
}

/**
 * Metadata når arbeidstaker fyller ut skjemaet selv.
 */
data class DegSelvMetadata(
    override val skjemadel: Skjemadel,
    override val arbeidsgiverNavn: String,
    override val juridiskEnhetOrgnr: String,
    override val kobletSkjemaId: java.util.UUID? = null,
    override val erstatterSkjemaId: java.util.UUID? = null
) : UtsendtArbeidstakerMetadata() {
    override val metadatatype: String = "UTSENDT_ARBEIDSTAKER_DEG_SELV"
    override val representasjonstype: Representasjonstype = Representasjonstype.DEG_SELV
    override fun medKobletSkjemaId(kobletSkjemaId: java.util.UUID?) = copy(kobletSkjemaId = kobletSkjemaId)
    override fun medErstatterSkjemaId(erstatterSkjemaId: java.util.UUID?) = copy(erstatterSkjemaId = erstatterSkjemaId)
}

/**
 * Metadata når arbeidsgiver fyller ut skjemaet uten fullmakt.
 */
data class ArbeidsgiverMetadata(
    override val skjemadel: Skjemadel,
    override val arbeidsgiverNavn: String,
    override val juridiskEnhetOrgnr: String,
    override val kobletSkjemaId: java.util.UUID? = null,
    override val erstatterSkjemaId: java.util.UUID? = null
) : UtsendtArbeidstakerMetadata() {
    override val metadatatype: String = "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER"
    override val representasjonstype: Representasjonstype = Representasjonstype.ARBEIDSGIVER
    override fun medKobletSkjemaId(kobletSkjemaId: java.util.UUID?) = copy(kobletSkjemaId = kobletSkjemaId)
    override fun medErstatterSkjemaId(erstatterSkjemaId: java.util.UUID?) = copy(erstatterSkjemaId = erstatterSkjemaId)
}

/**
 * Metadata når arbeidsgiver fyller ut skjemaet med fullmakt.
 */
data class ArbeidsgiverMedFullmaktMetadata(
    override val skjemadel: Skjemadel,
    override val arbeidsgiverNavn: String,
    override val juridiskEnhetOrgnr: String,
    /** Fødselsnummer til fullmektig (den som fyller ut på vegne av arbeidstaker) */
    val fullmektigFnr: String,
    override val kobletSkjemaId: java.util.UUID? = null,
    override val erstatterSkjemaId: java.util.UUID? = null
) : UtsendtArbeidstakerMetadata() {
    override val metadatatype: String = "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER_MED_FULLMAKT"
    override val representasjonstype: Representasjonstype = Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT
    override fun medKobletSkjemaId(kobletSkjemaId: java.util.UUID?) = copy(kobletSkjemaId = kobletSkjemaId)
    override fun medErstatterSkjemaId(erstatterSkjemaId: java.util.UUID?) = copy(erstatterSkjemaId = erstatterSkjemaId)
}

/**
 * Metadata når rådgiver fyller ut skjemaet uten fullmakt.
 */
data class RadgiverMetadata(
    override val skjemadel: Skjemadel,
    override val arbeidsgiverNavn: String,
    override val juridiskEnhetOrgnr: String,
    override val kobletSkjemaId: java.util.UUID? = null,
    override val erstatterSkjemaId: java.util.UUID? = null,
    /** Informasjon om rådgiverfirmaet */
    val radgiverfirma: RadgiverfirmaInfo
) : UtsendtArbeidstakerMetadata() {
    override val metadatatype: String = "UTSENDT_ARBEIDSTAKER_RADGIVER"
    override val representasjonstype: Representasjonstype = Representasjonstype.RADGIVER
    override fun medKobletSkjemaId(kobletSkjemaId: java.util.UUID?) = copy(kobletSkjemaId = kobletSkjemaId)
    override fun medErstatterSkjemaId(erstatterSkjemaId: java.util.UUID?) = copy(erstatterSkjemaId = erstatterSkjemaId)
}

/**
 * Metadata når rådgiver fyller ut skjemaet med fullmakt.
 */
data class RadgiverMedFullmaktMetadata(
    override val skjemadel: Skjemadel,
    override val arbeidsgiverNavn: String,
    override val juridiskEnhetOrgnr: String,
    /** Fødselsnummer til fullmektig (den som fyller ut på vegne av arbeidstaker) */
    val fullmektigFnr: String,
    override val kobletSkjemaId: java.util.UUID? = null,
    override val erstatterSkjemaId: java.util.UUID? = null,
    /** Informasjon om rådgiverfirmaet */
    val radgiverfirma: RadgiverfirmaInfo
) : UtsendtArbeidstakerMetadata() {
    override val metadatatype: String = "UTSENDT_ARBEIDSTAKER_RADGIVER_MED_FULLMAKT"
    override val representasjonstype: Representasjonstype = Representasjonstype.RADGIVER_MED_FULLMAKT
    override fun medKobletSkjemaId(kobletSkjemaId: java.util.UUID?) = copy(kobletSkjemaId = kobletSkjemaId)
    override fun medErstatterSkjemaId(erstatterSkjemaId: java.util.UUID?) = copy(erstatterSkjemaId = erstatterSkjemaId)
}

/**
 * Metadata når annen person med fullmakt fyller ut skjemaet.
 */
data class AnnenPersonMetadata(
    override val skjemadel: Skjemadel,
    override val arbeidsgiverNavn: String,
    override val juridiskEnhetOrgnr: String,
    /** Fødselsnummer til fullmektig (påkrevd for annen person) */
    val fullmektigFnr: String,
    override val kobletSkjemaId: java.util.UUID? = null,
    override val erstatterSkjemaId: java.util.UUID? = null
) : UtsendtArbeidstakerMetadata() {
    override val metadatatype: String = "UTSENDT_ARBEIDSTAKER_ANNEN_PERSON"
    override val representasjonstype: Representasjonstype = Representasjonstype.ANNEN_PERSON
    override fun medKobletSkjemaId(kobletSkjemaId: java.util.UUID?) = copy(kobletSkjemaId = kobletSkjemaId)
    override fun medErstatterSkjemaId(erstatterSkjemaId: java.util.UUID?) = copy(erstatterSkjemaId = erstatterSkjemaId)
}

data class RadgiverfirmaInfo(
    val orgnr: String,
    val navn: String
)

enum class Representasjonstype {
    DEG_SELV,
    ARBEIDSGIVER,
    ARBEIDSGIVER_MED_FULLMAKT,
    RADGIVER,
    RADGIVER_MED_FULLMAKT,
    ANNEN_PERSON
}

enum class Skjemadel {
    ARBEIDSTAKERS_DEL,
    ARBEIDSGIVERS_DEL
}
