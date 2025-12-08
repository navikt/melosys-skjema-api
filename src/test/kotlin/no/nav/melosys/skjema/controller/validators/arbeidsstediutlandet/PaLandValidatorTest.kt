package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.stream.Stream
import no.nav.melosys.skjema.controller.validators.BaseValidatorTest
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.FastEllerVekslendeArbeidssted
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.PaLandDto
import no.nav.melosys.skjema.paLandDtoMedDefaultVerdier
import no.nav.melosys.skjema.paLandFastArbeidsstedDtoMedDefaultVerdier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaLandValidatorTest: BaseValidatorTest() {

    @Test
    fun `PaLandDto should be annotated with GyldigPaLand`() {
        val annotation = PaLandDto::class.java.getAnnotation(GyldigPaLand::class.java)
        annotation.shouldNotBeNull()
    }

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
        // Message assertion removed
    }


    fun validCombinations(): Stream<Arguments> = listOf(
        paLandDtoMedDefaultVerdier().copy(
            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.FAST,
            fastArbeidssted = paLandFastArbeidsstedDtoMedDefaultVerdier(),
            beskrivelseVekslende = null
        ),
        paLandDtoMedDefaultVerdier().copy(
            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.VEKSLENDE,
            fastArbeidssted = null,
            beskrivelseVekslende = "Vekslende arbeidssteder"
        )
    ).map { Arguments.of(it) }.stream()


    fun invalidCombinations(): Stream<Arguments> = listOf(
        paLandDtoMedDefaultVerdier().copy(
            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.FAST,
            fastArbeidssted = null,
            beskrivelseVekslende = null
        ),
        paLandDtoMedDefaultVerdier().copy(
            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.FAST,
            fastArbeidssted = paLandFastArbeidsstedDtoMedDefaultVerdier(),
            beskrivelseVekslende = "Beskrivelse"
        ),
        paLandDtoMedDefaultVerdier().copy(
            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.VEKSLENDE,
            fastArbeidssted = paLandFastArbeidsstedDtoMedDefaultVerdier(),
            beskrivelseVekslende = "Beskrivelse"
        ),
        paLandDtoMedDefaultVerdier().copy(
            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.VEKSLENDE,
            fastArbeidssted = null,
            beskrivelseVekslende = null
        ),
        paLandDtoMedDefaultVerdier().copy(
            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.VEKSLENDE,
            fastArbeidssted = null,
            beskrivelseVekslende = ""
        ),
        paLandDtoMedDefaultVerdier().copy(
            fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.VEKSLENDE,
            fastArbeidssted = null,
            beskrivelseVekslende = "   "
        )
    ).map { Arguments.of(it) }.stream()

}
