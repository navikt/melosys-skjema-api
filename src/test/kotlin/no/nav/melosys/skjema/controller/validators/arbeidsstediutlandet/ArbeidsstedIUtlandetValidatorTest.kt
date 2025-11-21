package no.nav.melosys.skjema.controller.validators.arbeidsstediutlandet

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation
import jakarta.validation.Validator
import java.util.stream.Stream
import no.nav.melosys.skjema.arbeidsstedIUtlandetDtoMedDefaultVerdier
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedType
import no.nav.melosys.skjema.offshoreDtoMedDefaultVerdier
import no.nav.melosys.skjema.omBordPaFlyDtoMedDefaultVerdier
import no.nav.melosys.skjema.paLandDtoMedDefaultVerdier
import no.nav.melosys.skjema.paSkipDtoMedDefaultVerdier
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidsstedIUtlandetValidatorTest {

    private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

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
        violations.first().message.shouldBe("Ugyldig arbeidssted i utlandet")
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

}