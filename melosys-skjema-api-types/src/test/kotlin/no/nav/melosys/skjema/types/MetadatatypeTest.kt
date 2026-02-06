package no.nav.melosys.skjema.types

import com.fasterxml.jackson.annotation.JsonSubTypes
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/**
 * Tester at metadatatype-verdiene er stabile og konsistente.
 *
 * VIKTIG: metadatatype-verdiene lagres i databasen som JSONB.
 * Endring av verdier vil knekke deserialisering av eksisterende rader!
 */
class MetadatatypeTest {

    @Test
    fun `metadatatype-verdier skal være stabile og matche JsonSubTypes`() {
        val jsonSubTypesAnnotation = SkjemaMetadata::class.java.getAnnotation(JsonSubTypes::class.java)
        val mappings = jsonSubTypesAnnotation.value.associate { it.value.java to it.name }

        // VIKTIG: Disse verdiene lagres i databasen som JSONB.
        // IKKE endre eksisterende verdier - det vil knekke deserialisering!
        mappings[DegSelvMetadata::class.java] shouldBe DegSelvMetadata(
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
            arbeidsgiverNavn = "",
            juridiskEnhetOrgnr = ""
        ).metadatatype shouldBe "UTSENDT_ARBEIDSTAKER_DEG_SELV"

        mappings[ArbeidsgiverMetadata::class.java] shouldBe ArbeidsgiverMetadata(
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
            arbeidsgiverNavn = "",
            juridiskEnhetOrgnr = "",
            harFullmakt = false
        ).metadatatype shouldBe "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVER"

        mappings[RadgiverMetadata::class.java] shouldBe RadgiverMetadata(
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
            arbeidsgiverNavn = "",
            juridiskEnhetOrgnr = "",
            harFullmakt = false,
            radgiverfirma = RadgiverfirmaInfo(orgnr = "", navn = "")
        ).metadatatype shouldBe "UTSENDT_ARBEIDSTAKER_RADGIVER"

        mappings[AnnenPersonMetadata::class.java] shouldBe AnnenPersonMetadata(
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
            arbeidsgiverNavn = "",
            juridiskEnhetOrgnr = "",
            fullmektigFnr = ""
        ).metadatatype shouldBe "UTSENDT_ARBEIDSTAKER_ANNEN_PERSON"
    }
}
