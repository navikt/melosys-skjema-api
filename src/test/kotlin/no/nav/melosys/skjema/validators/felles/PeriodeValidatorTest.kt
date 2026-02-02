package no.nav.melosys.skjema.validators.felles

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import java.time.LocalDate
import java.util.stream.Stream
import no.nav.melosys.skjema.types.felles.PeriodeDto
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PeriodeValidatorTest {

    @ParameterizedTest
    @MethodSource("validPerioder")
    fun `should be valid for valid periods`(dto: PeriodeDto) {
        val violations = PeriodeValidator.validate(dto)
        violations.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidPerioder")
    fun `should be invalid for invalid periods`(dto: PeriodeDto) {
        val violations = PeriodeValidator.validate(dto)
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
