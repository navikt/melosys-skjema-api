package no.nav.melosys.skjema.types

import com.fasterxml.jackson.annotation.JsonSubTypes
import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.types.arbeidsgiver.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import org.junit.jupiter.api.Test

/**
 * Tester at type-verdiene for SkjemaData er stabile og konsistente.
 *
 * VIKTIG: type-verdiene lagres i databasen som JSONB.
 * Endring av verdier vil knekke deserialisering av eksisterende rader!
 *
 * HVIS DENNE TESTEN FEILER:
 * - IKKE endre testen for å få den til å passere
 * - Fiks main-koden slik at verdiene matcher det som er forventet her
 * - Hvis du faktisk MÅ endre verdiene, kreves en databasemigrering
 */
class SkjemaDataTypeTest {

    @Test
    fun `type-verdier skal være stabile - FIKS MAIN-KODE IKKE TESTEN ved feil`() {
        val jsonSubTypesAnnotation = SkjemaData::class.java.getAnnotation(JsonSubTypes::class.java)
        val mappings = jsonSubTypesAnnotation.value.associate { it.value.java to it.name }

        // VIKTIG: Disse verdiene lagres i databasen som JSONB.
        // IKKE endre eksisterende verdier - det vil knekke deserialisering!
        mappings[UtsendtArbeidstakerArbeidstakersSkjemaDataDto::class.java] shouldBe 
            UtsendtArbeidstakerArbeidstakersSkjemaDataDto().type shouldBe "UTSENDT_ARBEIDSTAKER_ARBEIDSTAKERS_DEL"

        mappings[UtsendtArbeidstakerArbeidsgiversSkjemaDataDto::class.java] shouldBe 
            UtsendtArbeidstakerArbeidsgiversSkjemaDataDto().type shouldBe "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVERS_DEL"
    }
}
