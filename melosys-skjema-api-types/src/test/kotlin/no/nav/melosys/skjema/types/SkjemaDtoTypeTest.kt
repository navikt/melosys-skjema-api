package no.nav.melosys.skjema.types

import com.fasterxml.jackson.annotation.JsonSubTypes
import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Tester at type-verdiene for SkjemaDto er stabile og konsistente.
 *
 * VIKTIG: type-verdiene lagres i databasen som JSONB.
 * Endring av verdier vil knekke deserialisering av eksisterende rader!
 *
 * HVIS DENNE TESTEN FEILER:
 * - IKKE endre testen for å få den til å passere
 * - Fiks main-koden slik at verdiene matcher det som er forventet her
 * - Hvis du faktisk MÅ endre verdiene, kreves en databasemigrering
 */
class SkjemaDtoTypeTest {

    @Test
    fun `type-verdier skal være stabile - FIKS MAIN-KODE IKKE TESTEN ved feil`() {
        val jsonSubTypesAnnotation = SkjemaDto::class.java.getAnnotation(JsonSubTypes::class.java)
        val mappings = jsonSubTypesAnnotation.value.associate { it.value.java to it.name }

        // VIKTIG: Disse verdiene lagres i databasen som JSONB.
        // IKKE endre eksisterende verdier - det vil knekke deserialisering!
        mappings[UtsendtArbeidstakerSkjemaDto::class.java] shouldBe UtsendtArbeidstakerSkjemaDto(
            id = UUID.randomUUID(),
            status = SkjemaStatus.UTKAST,
            fnr = "",
            orgnr = "",
            metadata = DegSelvMetadata(
                skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                arbeidsgiverNavn = "",
                juridiskEnhetOrgnr = ""
            ),
            data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
        ).type.name shouldBe "UTSENDT_ARBEIDSTAKER"
    }
}
