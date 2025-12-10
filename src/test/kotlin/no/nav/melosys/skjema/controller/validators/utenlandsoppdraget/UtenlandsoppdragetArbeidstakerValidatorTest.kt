package no.nav.melosys.skjema.controller.validators.utenlandsoppdraget

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import jakarta.validation.Valid
import java.time.LocalDate
import java.util.stream.Stream
import no.nav.melosys.skjema.controller.validators.BaseValidatorTest
import no.nav.melosys.skjema.dto.arbeidstaker.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelDto
import no.nav.melosys.skjema.dto.felles.PeriodeDto
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
        utsendelsePeriode = PeriodeDto(
            fraDato = LocalDate.of(2024, 1, 1),
            tilDato = LocalDate.of(2024, 12, 31)
        )
    )

    @Test
    fun `UtenlandsoppdragetArbeidstakersDelDto should be annotated with GyldigUtenlandsoppdragetArbeidstaker`() {
        val annotation = UtenlandsoppdragetArbeidstakersDelDto::class.java.getAnnotation(GyldigUtenlandsoppdragetArbeidstaker::class.java)
        annotation.shouldNotBeNull()
    }

    @Test
    fun `utsendelsePeriode field should be annotated with Valid`() {
        val field = UtenlandsoppdragetArbeidstakersDelDto::class.java.getDeclaredField("utsendelsePeriode")
        val annotation = field.getAnnotation(Valid::class.java)
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
    }

    fun validCombinations(): Stream<Arguments> = listOf(
        // Default verdier - alle felt gyldig
        utenlandsoppdragetArbeidstakerDtoMedGyldigeVerdier,
        // UtsendelsesLand med kort kode
        utenlandsoppdragetArbeidstakerDtoMedGyldigeVerdier.copy(
            utsendelsesLand = "NO"
        )
    ).map { Arguments.of(it) }.stream()

    fun invalidCombinations(): Stream<Arguments> = listOf<UtenlandsoppdragetArbeidstakersDelDto>(
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
