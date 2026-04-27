package no.nav.melosys.skjema.validators.arbeidsstediutlandet

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import java.util.stream.Stream
import no.nav.melosys.skjema.paLandDtoMedDefaultVerdier
import no.nav.melosys.skjema.paLandFastArbeidsstedDtoMedDefaultVerdier
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FastEllerVekslendeArbeidssted
import no.nav.melosys.skjema.types.utsendtarbeidstaker.PaLandDto
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaLandValidatorTest {

    private val validator = PaLandValidator()

    @ParameterizedTest
    @MethodSource("validCombinations")
    fun `should be valid for valid combinations`(dto: PaLandDto) {
        val violations = validator.validate(dto)
        violations.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun `should be invalid for invalid combinations`(dto: PaLandDto) {
        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
    }


    fun validCombinations(): Stream<Arguments> = listOf(
        paLandDtoMedDefaultVerdier().copy(
            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.FAST,
            fastArbeidssted = paLandFastArbeidsstedDtoMedDefaultVerdier()
        ),
        paLandDtoMedDefaultVerdier().copy(
            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.VEKSLENDE,
            fastArbeidssted = null
        )
    ).map { Arguments.of(it) }.stream()


    fun invalidCombinations(): Stream<Arguments> = listOf(
        paLandDtoMedDefaultVerdier().copy(
            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.FAST,
            fastArbeidssted = null
        ),
        paLandDtoMedDefaultVerdier().copy(
            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.VEKSLENDE,
            fastArbeidssted = paLandFastArbeidsstedDtoMedDefaultVerdier()
        )
    ).map { Arguments.of(it) }.stream()

}
