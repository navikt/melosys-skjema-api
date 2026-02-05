package no.nav.melosys.skjema.types

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Base sealed class for all skjema metadata types.
 * Lagres i skjema.metadata som JSONB.
 *
 * Diskrimineres basert på `metadatatype`:
 * - [DegSelvMetadata] for UTSENDT_ARBEIDSTAKER_DEG_SELV
 * - [ArbeidsgiverMetadata] for UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER
 * - [RadgiverMetadata] for UTSENDT_ARBEIDSTAKER_RADGIVER
 * - [AnnenPersonMetadata] for UTSENDT_ARBEIDSTAKER_ANNEN_PERSON
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "metadatatype"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = DegSelvMetadata::class, name = "UTSENDT_ARBEIDSTAKER_DEG_SELV"),
    JsonSubTypes.Type(value = ArbeidsgiverMetadata::class, name = "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER"),
    JsonSubTypes.Type(value = RadgiverMetadata::class, name = "UTSENDT_ARBEIDSTAKER_RADGIVER"),
    JsonSubTypes.Type(value = AnnenPersonMetadata::class, name = "UTSENDT_ARBEIDSTAKER_ANNEN_PERSON")
)
sealed class SkjemaMetadata {
    /** Skjematype for denne metadataen */
    abstract val skjemaType: SkjemaType

    /** Diskriminator for Jackson-serialisering. Skal ikke endres - lagres i database. */
    abstract val metadatatype: String
}
