package no.nav.melosys.skjema.controller.validators.utenlandsoppdraget

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.util.stream.Stream
import no.nav.melosys.skjema.controller.validators.BaseValidatorTest
import no.nav.melosys.skjema.dto.arbeidstaker.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelDto
import no.nav.melosys.skjema.utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtenlandsoppdragetArbeidstakerValidatorTest : BaseValidatorTest() {

    private val utenlandsoppdragetArbeidstakerDtoMedGyldigeVerdier = utenlandsoppdragetArbeidstakersDelDtoMedDefaultVerdier().copy(
        utsendelsesLand = "SE",
        utsendelseFraDato = LocalDate.of(2024, 1, 1),
        utsendelseTilDato = LocalDate.of(2024, 12, 31)
    )

    @Test
    fun `UtenlandsoppdragetArbeidstakersDelDto should be annotated with GyldigUtenlandsoppdragetArbeidstaker`() {
        val annotation = UtenlandsoppdragetArbeidstakersDelDto::class.java.getAnnotation(GyldigUtenlandsoppdragetArbeidstaker::class.java)
        annotation.shouldNotBeNull()
    }

    @ParameterizedTest
    @MethodSource("validCombinations")
    fun `should be valid for valid combinations`(dto: UtenlandsoppdragetArbeidstakersDelDto) {
        val violations = validator.validate(dto)
        violations.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun `should be invalid for invalid combinations`(dto: UtenlandsoppdragetArbeidstakersDelDto) {
        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
        violations.first().message.shouldBe("Ugyldig utenlandsoppdrag arbeidstaker")
    }

    fun validCombinations(): Stream<Arguments> = listOf(
        // Default verdier - alle felt gyldig
        utenlandsoppdragetArbeidstakerDtoMedGyldigeVerdier,
        // FraDato samme som tilDato
        utenlandsoppdragetArbeidstakerDtoMedGyldigeVerdier.copy(
            utsendelseFraDato = LocalDate.of(2024, 6, 15),
            utsendelseTilDato = LocalDate.of(2024, 6, 15)
        ),
        // UtsendelsesLand med kort kode
        utenlandsoppdragetArbeidstakerDtoMedGyldigeVerdier.copy(
            utsendelsesLand = "NO"
        )
    ).map { Arguments.of(it) }.stream()

    fun invalidCombinations(): Stream<Arguments> = listOf<UtenlandsoppdragetArbeidstakersDelDto>(
        // FraDato er etter tilDato
        utenlandsoppdragetArbeidstakerDtoMedGyldigeVerdier.copy(
            utsendelseFraDato = LocalDate.of(2024, 12, 31),
            utsendelseTilDato = LocalDate.of(2024, 1, 1)
        ),
        // UtsendelsesLand er tom string
        utenlandsoppdragetArbeidstakerDtoMedGyldigeVerdier.copy(
            utsendelsesLand = ""
        ),
        // UtsendelsesLand er blank (kun whitespace)
        utenlandsoppdragetArbeidstakerDtoMedGyldigeVerdier.copy(
            utsendelsesLand = "   "
        )
    ).map { Arguments.of(it) }.stream()
}
