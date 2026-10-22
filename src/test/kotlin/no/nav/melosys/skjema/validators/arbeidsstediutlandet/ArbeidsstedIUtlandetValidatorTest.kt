package no.nav.melosys.skjema.validators.arbeidsstediutlandet

import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.validators.Violation
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import java.util.stream.Stream
import no.nav.melosys.skjema.arbeidsstedIUtlandetDtoMedDefaultVerdier
import no.nav.melosys.skjema.offshoreDtoMedDefaultVerdier
import no.nav.melosys.skjema.omBordPaFlyDtoMedDefaultVerdier
import no.nav.melosys.skjema.paLandDtoMedDefaultVerdier
import no.nav.melosys.skjema.paSkipDtoMedDefaultVerdier
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Farvann
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FastEllerVekslendeArbeidssted
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidsstedIUtlandetValidatorTest {

    private val validator = ArbeidsstedIUtlandetValidator(
        OmBordPaFlyValidator(),
        PaLandValidator(),
        PaSkipValidator()
    )

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
                fastArbeidssted = null
            ),
            offshore = null,
            paSkip = null,
            omBordPaFly = null
        )

        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
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
    }

    @Test
    fun `forenklet arbeidssted godtar bare på land uten hjemmekontor`() {
        val paLandUtenHjemmekontor = arbeidsstedIUtlandetDtoMedDefaultVerdier().copy(
            paLand = paLandDtoMedDefaultVerdier().copy(erHjemmekontor = null)
        )
        val offshore = ArbeidsstedIUtlandetDto(arbeidsstedType = ArbeidsstedType.OFFSHORE, offshore = offshoreDtoMedDefaultVerdier())

        validator.validate(paLandUtenHjemmekontor, forenklet = true).shouldBeEmpty()
        validator.validate(offshore, forenklet = true) shouldBe listOf(
            Violation(field = "paLand", translationKey = "arbeidsstedIUtlandetTranslation.maaOppgiArbeidsstedPaLand")
        )
        validator.validate(arbeidsstedIUtlandetDtoMedDefaultVerdier(), forenklet = true) shouldBe listOf(
            Violation(field = "erHjemmekontor", translationKey = "paLandTranslation.hjemmekontorSkalIkkeOppgis")
        )
    }

    @Test
    fun `arbeidssted skal ikke oppgis når steget ikke er i flyten`() {
        validator.validate(null, skalOppgis = false).shouldBeEmpty()
        validator.validate(arbeidsstedIUtlandetDtoMedDefaultVerdier(), skalOppgis = false) shouldBe listOf(
            Violation(field = "arbeidsstedIUtlandet", translationKey = "arbeidsstedIUtlandetTranslation.arbeidsstedSkalIkkeOppgis")
        )
    }
}
