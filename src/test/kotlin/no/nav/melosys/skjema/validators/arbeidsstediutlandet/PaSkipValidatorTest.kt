package no.nav.melosys.skjema.validators.arbeidsstediutlandet

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import java.util.stream.Stream
import no.nav.melosys.skjema.paSkipDtoMedDefaultVerdier
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.Farvann
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.PaSkipDto
import no.nav.melosys.skjema.types.felles.LandKode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaSkipValidatorTest {

    private val validator = PaSkipValidator()

    @ParameterizedTest
    @MethodSource("validCombinations")
    fun `should be valid for valid combinations`(dto: PaSkipDto) {
        val violations = validator.validate(dto)
        violations.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun `should be invalid for invalid combinations`(dto: PaSkipDto) {
        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
    }

    fun validCombinations(): Stream<Arguments> = listOf(
        paSkipDtoMedDefaultVerdier().copy(
            seilerI = Farvann.INTERNASJONALT_FARVANN,
            flaggland = LandKode.SE,
            territorialfarvannLand = null
        ),
        paSkipDtoMedDefaultVerdier().copy(
            seilerI = Farvann.TERRITORIALFARVANN,
            flaggland = null,
            territorialfarvannLand = LandKode.SE
        )
    ).map { Arguments.of(it) }.stream()

    fun invalidCombinations(): Stream<Arguments> = listOf(
        paSkipDtoMedDefaultVerdier().copy(
            seilerI = Farvann.INTERNASJONALT_FARVANN,
            flaggland = null,
            territorialfarvannLand = null
        ),
        paSkipDtoMedDefaultVerdier().copy(
            seilerI = Farvann.INTERNASJONALT_FARVANN,
            flaggland = LandKode.SE,
            territorialfarvannLand = LandKode.SE
        ),
        paSkipDtoMedDefaultVerdier().copy(
            seilerI = Farvann.TERRITORIALFARVANN,
            flaggland = null,
            territorialfarvannLand = null
        ),
        paSkipDtoMedDefaultVerdier().copy(
            seilerI = Farvann.TERRITORIALFARVANN,
            flaggland = LandKode.SE,
            territorialfarvannLand = LandKode.SE
        )
    ).map { Arguments.of(it) }.stream()
}
