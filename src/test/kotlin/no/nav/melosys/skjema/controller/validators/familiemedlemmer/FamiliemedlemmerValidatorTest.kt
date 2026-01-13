package no.nav.melosys.skjema.controller.validators.familiemedlemmer

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import java.util.stream.Stream
import no.nav.melosys.skjema.controller.validators.BaseValidatorTest
import no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.familiemedlemMedDefaultVerdier
import no.nav.melosys.skjema.familiemedlemmerDtoMedDefaultVerdier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FamiliemedlemmerValidatorTest : BaseValidatorTest() {

    @Test
    fun `FamiliemedlemmerDto should be annotated with GyldigFamiliemedlemmer`() {
        val annotation = FamiliemedlemmerDto::class.java.getAnnotation(GyldigFamiliemedlemmer::class.java)
        annotation.shouldNotBeNull()
    }

    @ParameterizedTest
    @MethodSource("validCombinations")
    fun `should be valid for valid combinations`(dto: FamiliemedlemmerDto) {
        val violations = validator.validate(dto)
        violations.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun `should be invalid for invalid combinations`(dto: FamiliemedlemmerDto) {
        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
    }

    fun validCombinations(): Stream<Arguments> = listOf(
        // Skal ikke ha med familiemedlemmer, listen er tom
        familiemedlemmerDtoMedDefaultVerdier().copy(
            skalHaMedFamiliemedlemmer = false,
            familiemedlemmer = emptyList()
        ),
        // Skal ha med familiemedlemmer, listen har ett medlem
        familiemedlemmerDtoMedDefaultVerdier().copy(
            skalHaMedFamiliemedlemmer = true,
            familiemedlemmer = listOf(familiemedlemMedDefaultVerdier())
        ),
        // Skal ha med familiemedlemmer, listen har flere medlemmer
        familiemedlemmerDtoMedDefaultVerdier().copy(
            skalHaMedFamiliemedlemmer = true,
            familiemedlemmer = listOf(
                familiemedlemMedDefaultVerdier(),
                familiemedlemMedDefaultVerdier().copy(fornavn = "Jane")
            )
        )
    ).map { Arguments.of(it) }.stream()

    fun invalidCombinations(): Stream<Arguments> = listOf(
        // Skal ikke ha med familiemedlemmer, men listen er ikke tom
        familiemedlemmerDtoMedDefaultVerdier().copy(
            skalHaMedFamiliemedlemmer = false,
            familiemedlemmer = listOf(familiemedlemMedDefaultVerdier())
        )
    ).map { Arguments.of(it) }.stream()
}
