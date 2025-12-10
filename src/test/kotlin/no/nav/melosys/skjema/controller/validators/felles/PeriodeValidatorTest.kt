package no.nav.melosys.skjema.controller.validators.felles

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.util.stream.Stream
import no.nav.melosys.skjema.controller.validators.BaseValidatorTest
import no.nav.melosys.skjema.dto.felles.PeriodeDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PeriodeValidatorTest : BaseValidatorTest() {

    @Test
    fun `PeriodeDto should be annotated with GyldigPeriode`() {
        val annotation = PeriodeDto::class.java.getAnnotation(GyldigPeriode::class.java)
        annotation.shouldNotBeNull()
    }

    @ParameterizedTest
    @MethodSource("validPerioder")
    fun `should be valid for valid periods`(dto: PeriodeDto) {
        val violations = validator.validate(dto)
        violations.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidPerioder")
    fun `should be invalid for invalid periods`(dto: PeriodeDto) {
        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
    }

    fun validPerioder(): Stream<Arguments> = listOf(
        // FraDato før tilDato
        PeriodeDto(
            fraDato = LocalDate.of(2024, 1, 1),
            tilDato = LocalDate.of(2024, 12, 31)
        ),
        // FraDato samme som tilDato
        PeriodeDto(
            fraDato = LocalDate.of(2024, 6, 15),
            tilDato = LocalDate.of(2024, 6, 15)
        )
    ).map { Arguments.of(it) }.stream()

    fun invalidPerioder(): Stream<Arguments> = listOf(
        // FraDato etter tilDato
        PeriodeDto(
            fraDato = LocalDate.of(2024, 12, 31),
            tilDato = LocalDate.of(2024, 1, 1)
        )
    ).map { Arguments.of(it) }.stream()
}
