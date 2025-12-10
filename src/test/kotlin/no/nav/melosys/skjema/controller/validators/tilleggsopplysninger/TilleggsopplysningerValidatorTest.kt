package no.nav.melosys.skjema.controller.validators.tilleggsopplysninger

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.stream.Stream
import no.nav.melosys.skjema.controller.validators.BaseValidatorTest
import no.nav.melosys.skjema.dto.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.tilleggsopplysningerDtoMedDefaultVerdier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TilleggsopplysningerValidatorTest : BaseValidatorTest() {

    @Test
    fun `TilleggsopplysningerDto should be annotated with GyldigTilleggsopplysninger`() {
        val annotation = TilleggsopplysningerDto::class.java.getAnnotation(GyldigTilleggsopplysninger::class.java)
        annotation.shouldNotBeNull()
    }

    @ParameterizedTest
    @MethodSource("validCombinations")
    fun `should be valid for valid combinations`(dto: TilleggsopplysningerDto) {
        val violations = validator.validate(dto)
        violations.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun `should be invalid for invalid combinations`(dto: TilleggsopplysningerDto) {
        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
    }

    fun validCombinations(): Stream<Arguments> = listOf(
        // Har ikke flere opplysninger - felt må være null
        tilleggsopplysningerDtoMedDefaultVerdier().copy(
            harFlereOpplysningerTilSoknaden = false,
            tilleggsopplysningerTilSoknad = null
        ),
        // Har flere opplysninger - felt er satt
        tilleggsopplysningerDtoMedDefaultVerdier().copy(
            harFlereOpplysningerTilSoknaden = true,
            tilleggsopplysningerTilSoknad = "Noen tilleggsopplysninger"
        )
    ).map { Arguments.of(it) }.stream()

    fun invalidCombinations(): Stream<Arguments> = listOf(
        // Har ikke flere opplysninger, men felt er satt
        tilleggsopplysningerDtoMedDefaultVerdier().copy(
            harFlereOpplysningerTilSoknaden = false,
            tilleggsopplysningerTilSoknad = "Noen opplysninger"
        ),
        // Har ikke flere opplysninger, men felt er tomt (må være null, ikke tom streng)
        tilleggsopplysningerDtoMedDefaultVerdier().copy(
            harFlereOpplysningerTilSoknaden = false,
            tilleggsopplysningerTilSoknad = ""
        ),
        // Har ikke flere opplysninger, men felt er blank (må være null, ikke blank streng)
        tilleggsopplysningerDtoMedDefaultVerdier().copy(
            harFlereOpplysningerTilSoknaden = false,
            tilleggsopplysningerTilSoknad = "   "
        ),
        // Har flere opplysninger, men felt er null
        tilleggsopplysningerDtoMedDefaultVerdier().copy(
            harFlereOpplysningerTilSoknaden = true,
            tilleggsopplysningerTilSoknad = null
        ),
        // Har flere opplysninger, men felt er tomt
        tilleggsopplysningerDtoMedDefaultVerdier().copy(
            harFlereOpplysningerTilSoknaden = true,
            tilleggsopplysningerTilSoknad = ""
        ),
        // Har flere opplysninger, men felt er blank
        tilleggsopplysningerDtoMedDefaultVerdier().copy(
            harFlereOpplysningerTilSoknaden = true,
            tilleggsopplysningerTilSoknad = "   "
        )
    ).map { Arguments.of(it) }.stream()
}
