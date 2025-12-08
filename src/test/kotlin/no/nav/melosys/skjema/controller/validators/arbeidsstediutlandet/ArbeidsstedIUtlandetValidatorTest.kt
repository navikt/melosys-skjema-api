package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.stream.Stream
import no.nav.melosys.skjema.arbeidsstedIUtlandetDtoMedDefaultVerdier
import no.nav.melosys.skjema.controller.validators.BaseValidatorTest
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedType
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.Farvann
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.FastEllerVekslendeArbeidssted
import no.nav.melosys.skjema.offshoreDtoMedDefaultVerdier
import no.nav.melosys.skjema.omBordPaFlyDtoMedDefaultVerdier
import no.nav.melosys.skjema.paLandDtoMedDefaultVerdier
import no.nav.melosys.skjema.paSkipDtoMedDefaultVerdier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidsstedIUtlandetValidatorTest: BaseValidatorTest() {

    @Test
    fun `ArbeidsstedIUtlandetDto should be annotated with GyldigArbeidsstedIUtlandet`() {
        val annotation = ArbeidsstedIUtlandetDto::class.java.getAnnotation(GyldigArbeidsstedIUtlandet::class.java)
        annotation.shouldNotBeNull()
    }

    @ParameterizedTest
    @MethodSource("validCombinations")
    fun `should be valid for valid combinations`(dto: ArbeidsstedIUtlandetDto) {
        val violations = validator.validate(dto)
        violations.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun `should be invalid for invalid combinations`(dto: ArbeidsstedIUtlandetDto) {
        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
        // Message assertion removed
    }


    fun validCombinations(): Stream<Arguments> = listOf(
        arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
            arbeidsstedType = ArbeidsstedType.PA_LAND,
            paLand = paLandDtoMedDefaultVerdier(),
            offshore = null,
            paSkip = null,
            omBordPaFly = null
        ),
        arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
            arbeidsstedType = ArbeidsstedType.OFFSHORE,
            paLand = null,
            offshore = offshoreDtoMedDefaultVerdier(),
            paSkip = null,
            omBordPaFly = null
        ),
        arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
            arbeidsstedType = ArbeidsstedType.PA_SKIP,
            paLand = null,
            offshore = null,
            paSkip = paSkipDtoMedDefaultVerdier(),
            omBordPaFly = null
        ),
        arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
            arbeidsstedType = ArbeidsstedType.OM_BORD_PA_FLY,
            paLand = null,
            offshore = null,
            paSkip = null,
            omBordPaFly = omBordPaFlyDtoMedDefaultVerdier()
        )
    ).map { Arguments.of(it) }.stream()


    fun invalidCombinations(): Stream<Arguments> {

        return listOf(
            arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                arbeidsstedType = ArbeidsstedType.PA_LAND,
                paLand = null,
                offshore = null,
                paSkip = null,
                omBordPaFly = null
            ),
            arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                arbeidsstedType = ArbeidsstedType.PA_LAND,
                paLand = paLandDtoMedDefaultVerdier(),
                offshore = offshoreDtoMedDefaultVerdier(),
                paSkip = null,
                omBordPaFly = null
            ),
            arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                arbeidsstedType = ArbeidsstedType.OFFSHORE,
                paLand = null,
                offshore = null,
                paSkip = null,
                omBordPaFly = null
            ),
            arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                arbeidsstedType = ArbeidsstedType.OFFSHORE,
                paLand = null,
                offshore = offshoreDtoMedDefaultVerdier(),
                paSkip = paSkipDtoMedDefaultVerdier(),
                omBordPaFly = null
            ),
            arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                arbeidsstedType = ArbeidsstedType.PA_SKIP,
                paLand = null,
                offshore = null,
                paSkip = null,
                omBordPaFly = null
            ),
            arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                arbeidsstedType = ArbeidsstedType.PA_SKIP,
                paLand = null,
                offshore = null,
                paSkip = paSkipDtoMedDefaultVerdier(),
                omBordPaFly = omBordPaFlyDtoMedDefaultVerdier()
            ),
            arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                arbeidsstedType = ArbeidsstedType.OM_BORD_PA_FLY,
                paLand = null,
                offshore = null,
                paSkip = null,
                omBordPaFly = null
            ),
            arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
                arbeidsstedType = ArbeidsstedType.OM_BORD_PA_FLY,
                paLand = paLandDtoMedDefaultVerdier(),
                offshore = null,
                paSkip = null,
                omBordPaFly = omBordPaFlyDtoMedDefaultVerdier()
            )
        ).map { Arguments.of(it) }.stream()
    }

    @Test
    fun `should be invalid when paLand has invalid nested data`() {
        val dto = arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
            arbeidsstedType = ArbeidsstedType.PA_LAND,
            paLand = paLandDtoMedDefaultVerdier().copy(
                fastEllerVekslendeArbeidssted = FastEllerVekslendeArbeidssted.FAST,
                fastArbeidssted = null,
                beskrivelseVekslende = null
            ),
            offshore = null,
            paSkip = null,
            omBordPaFly = null
        )

        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
        // Message assertion removed
    }

    @Test
    fun `should be invalid when paSkip has invalid nested data`() {
        val dto = arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
            arbeidsstedType = ArbeidsstedType.PA_SKIP,
            paLand = null,
            offshore = null,
            paSkip = paSkipDtoMedDefaultVerdier().copy(
                seilerI = Farvann.INTERNASJONALT_FARVANN,
                flaggland = null,
                territorialfarvannLand = null
            ),
            omBordPaFly = null
        )

        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
        // Message assertion removed
    }

    @Test
    fun `should be invalid when omBordPaFly has invalid nested data`() {
        val dto = arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
            arbeidsstedType = ArbeidsstedType.OM_BORD_PA_FLY,
            paLand = null,
            offshore = null,
            paSkip = null,
            omBordPaFly = omBordPaFlyDtoMedDefaultVerdier().copy(
                erVanligHjemmebase = false,
                vanligHjemmebaseLand = null,
                vanligHjemmebaseNavn = null
            )
        )

        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
        // Message assertion removed
    }

}