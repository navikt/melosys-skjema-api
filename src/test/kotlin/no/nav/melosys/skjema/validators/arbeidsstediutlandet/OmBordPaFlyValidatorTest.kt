package no.nav.melosys.skjema.validators.arbeidsstediutlandet

import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.validators.FELT_ER_PAAKREVD
import no.nav.melosys.skjema.validators.Violation
import org.junit.jupiter.api.Test
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import java.util.stream.Stream
import no.nav.melosys.skjema.omBordPaFlyDtoMedDefaultVerdier
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OmBordPaFlyDto
import no.nav.melosys.skjema.types.felles.LandKode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OmBordPaFlyValidatorTest {

    private val validator = OmBordPaFlyValidator()

    @ParameterizedTest
    @MethodSource("validCombinations")
    fun `should be valid for valid combinations`(dto: OmBordPaFlyDto) {
        val violations = validator.validate(dto)
        violations.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun `should be invalid for invalid combinations`(dto: OmBordPaFlyDto) {
        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
    }

    fun validCombinations(): Stream<Arguments> = listOf(
        omBordPaFlyDtoMedDefaultVerdier().copy(
            erVanligHjemmebase = true,
            vanligHjemmebaseLand = null,
            vanligHjemmebaseNavn = null
        ),
        omBordPaFlyDtoMedDefaultVerdier().copy(
            erVanligHjemmebase = false,
            vanligHjemmebaseLand = LandKode.SE,
            vanligHjemmebaseNavn = "Stockholm Airport"
        )
    ).map { Arguments.of(it) }.stream()

    fun invalidCombinations(): Stream<Arguments> = listOf(
        omBordPaFlyDtoMedDefaultVerdier().copy(
            erVanligHjemmebase = true,
            vanligHjemmebaseLand = LandKode.SE,
            vanligHjemmebaseNavn = null
        ),
        omBordPaFlyDtoMedDefaultVerdier().copy(
            erVanligHjemmebase = true,
            vanligHjemmebaseLand = null,
            vanligHjemmebaseNavn = "Stockholm Airport"
        ),
        omBordPaFlyDtoMedDefaultVerdier().copy(
            erVanligHjemmebase = true,
            vanligHjemmebaseLand = LandKode.SE,
            vanligHjemmebaseNavn = "Stockholm Airport"
        ),
        omBordPaFlyDtoMedDefaultVerdier().copy(
            erVanligHjemmebase = false,
            vanligHjemmebaseLand = null,
            vanligHjemmebaseNavn = null
        ),
        omBordPaFlyDtoMedDefaultVerdier().copy(
            erVanligHjemmebase = false,
            vanligHjemmebaseLand = LandKode.SE,
            vanligHjemmebaseNavn = null
        ),
        omBordPaFlyDtoMedDefaultVerdier().copy(
            erVanligHjemmebase = false,
            vanligHjemmebaseLand = null,
            vanligHjemmebaseNavn = "Stockholm Airport"
        )
    ).map { Arguments.of(it) }.stream()

    @Test
    fun `navn på hjemmebase og virksomhet kan ikke være blanke`() {
        validator.validate(omBordPaFlyDtoMedDefaultVerdier().copy(hjemmebaseNavn = "  ")) shouldBe listOf(
            Violation(field = "hjemmebaseNavn", translationKey = FELT_ER_PAAKREVD)
        )
        validator.validate(omBordPaFlyDtoMedDefaultVerdier().copy(navnPaVirksomhet = "")) shouldBe listOf(
            Violation(field = "navnPaVirksomhet", translationKey = FELT_ER_PAAKREVD)
        )
    }

    @Test
    fun `navn på vanlig hjemmebase kan ikke være blankt`() {
        val dto = omBordPaFlyDtoMedDefaultVerdier().copy(
            erVanligHjemmebase = false,
            vanligHjemmebaseLand = LandKode.SE,
            vanligHjemmebaseNavn = " "
        )

        validator.validate(dto) shouldBe listOf(
            Violation(field = "vanligHjemmebaseNavn", translationKey = "omBordPaFlyTranslation.maaOppgiVanligHjemmebaseNavn")
        )
    }
}
