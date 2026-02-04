package no.nav.melosys.skjema.validators.skatteforholdoginntekt

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import java.util.stream.Stream
import no.nav.melosys.skjema.skatteforholdOgInntektDtoMedDefaultVerdier
import no.nav.melosys.skjema.types.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SkatteforholdOgInntektValidatorTest {

    private val validator = SkatteforholdOgInntektValidator()

    @ParameterizedTest
    @MethodSource("validCombinations")
    fun `should be valid for valid combinations`(dto: SkatteforholdOgInntektDto) {
        val violations = validator.validate(dto)
        violations.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun `should be invalid for invalid combinations`(dto: SkatteforholdOgInntektDto) {
        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
    }

    fun validCombinations(): Stream<Arguments> = listOf(
        // Mottar ikke pengestøtte - alle felt kan være null
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = false,
            landSomUtbetalerPengestotte = null,
            pengestotteSomMottasFraAndreLandBelop = null,
            pengestotteSomMottasFraAndreLandBeskrivelse = null
        ),
        // Mottar ikke pengestøtte - felt kan være satt likevel (validatoren sjekker ikke dette)
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = false,
            landSomUtbetalerPengestotte = "NO",
            pengestotteSomMottasFraAndreLandBelop = "10000",
            pengestotteSomMottasFraAndreLandBeskrivelse = "Beskrivelse"
        ),
        // Mottar pengestøtte - alle påkrevde felt er satt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = "SE",
            pengestotteSomMottasFraAndreLandBelop = "5000",
            pengestotteSomMottasFraAndreLandBeskrivelse = "Studentstøtte fra Sverige"
        )
    ).map { Arguments.of(it) }.stream()

    fun invalidCombinations(): Stream<Arguments> = listOf(
        // Mottar pengestøtte, men landSomUtbetalerPengestotte er null
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = null,
            pengestotteSomMottasFraAndreLandBelop = "5000",
            pengestotteSomMottasFraAndreLandBeskrivelse = "Beskrivelse"
        ),
        // Mottar pengestøtte, men landSomUtbetalerPengestotte er blank
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = "   ",
            pengestotteSomMottasFraAndreLandBelop = "5000",
            pengestotteSomMottasFraAndreLandBeskrivelse = "Beskrivelse"
        ),
        // Mottar pengestøtte, men pengestotteSomMottasFraAndreLandBelop er null
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = "SE",
            pengestotteSomMottasFraAndreLandBelop = null,
            pengestotteSomMottasFraAndreLandBeskrivelse = "Beskrivelse"
        ),
        // Mottar pengestøtte, men pengestotteSomMottasFraAndreLandBelop er blank
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = "SE",
            pengestotteSomMottasFraAndreLandBelop = "   ",
            pengestotteSomMottasFraAndreLandBeskrivelse = "Beskrivelse"
        ),
        // Mottar pengestøtte, men pengestotteSomMottasFraAndreLandBeskrivelse er null
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = "SE",
            pengestotteSomMottasFraAndreLandBelop = "5000",
            pengestotteSomMottasFraAndreLandBeskrivelse = null
        ),
        // Mottar pengestøtte, men pengestotteSomMottasFraAndreLandBeskrivelse er blank
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = "SE",
            pengestotteSomMottasFraAndreLandBelop = "5000",
            pengestotteSomMottasFraAndreLandBeskrivelse = "   "
        ),
        // Mottar pengestøtte, men alle felt er null
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = null,
            pengestotteSomMottasFraAndreLandBelop = null,
            pengestotteSomMottasFraAndreLandBeskrivelse = null
        ),
        // Mottar pengestøtte, men kun land er satt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = "SE",
            pengestotteSomMottasFraAndreLandBelop = null,
            pengestotteSomMottasFraAndreLandBeskrivelse = null
        ),
        // Mottar pengestøtte, men kun beløp er satt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = null,
            pengestotteSomMottasFraAndreLandBelop = "5000",
            pengestotteSomMottasFraAndreLandBeskrivelse = null
        ),
        // Mottar pengestøtte, men kun beskrivelse er satt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = null,
            pengestotteSomMottasFraAndreLandBelop = null,
            pengestotteSomMottasFraAndreLandBeskrivelse = "Beskrivelse"
        )
    ).map { Arguments.of(it) }.stream()
}
