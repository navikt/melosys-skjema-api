package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.stream.Stream
import no.nav.melosys.skjema.controller.validators.BaseValidatorTest
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.Farvann
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.PaSkipDto
import no.nav.melosys.skjema.paSkipDtoMedDefaultVerdier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaSkipValidatorTest: BaseValidatorTest() {

    @Test
    fun `PaSkipDto should be annotated with GyldigPaSkip`() {
        val annotation = PaSkipDto::class.java.getAnnotation(GyldigPaSkip::class.java)
        annotation.shouldNotBeNull()
    }

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
            flaggland = "NO",
            territorialfarvannLand = null
        ),
        paSkipDtoMedDefaultVerdier().copy(
            seilerI = Farvann.TERRITORIALFARVANN,
            flaggland = null,
            territorialfarvannLand = "SE"
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
            flaggland = "",
            territorialfarvannLand = null
        ),
        paSkipDtoMedDefaultVerdier().copy(
            seilerI = Farvann.INTERNASJONALT_FARVANN,
            flaggland = "   ",
            territorialfarvannLand = null
        ),
        paSkipDtoMedDefaultVerdier().copy(
            seilerI = Farvann.INTERNASJONALT_FARVANN,
            flaggland = "NO",
            territorialfarvannLand = "SE"
        ),
        paSkipDtoMedDefaultVerdier().copy(
            seilerI = Farvann.TERRITORIALFARVANN,
            flaggland = null,
            territorialfarvannLand = null
        ),
        paSkipDtoMedDefaultVerdier().copy(
            seilerI = Farvann.TERRITORIALFARVANN,
            flaggland = null,
            territorialfarvannLand = ""
        ),
        paSkipDtoMedDefaultVerdier().copy(
            seilerI = Farvann.TERRITORIALFARVANN,
            flaggland = null,
            territorialfarvannLand = "   "
        ),
        paSkipDtoMedDefaultVerdier().copy(
            seilerI = Farvann.TERRITORIALFARVANN,
            flaggland = "NO",
            territorialfarvannLand = "SE"
        )
    ).map { Arguments.of(it) }.stream()
}
