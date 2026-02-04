package no.nav.melosys.skjema.validators.arbeidstakerenslonn

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.stream.Stream
import no.nav.melosys.skjema.arbeidstakerensLonnDtoMedDefaultVerdier
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.norskVirksomhetMedDefaultVerdier
import no.nav.melosys.skjema.norskeOgUtenlandskeVirksomheterMedDefaultVerdier
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.validators.felles.OrganisasjonsnummerValidator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidstakerensLonnValidatorTest {

    private val eregService = mockk<EregService>()
    private val validator = ArbeidstakerensLonnValidator(OrganisasjonsnummerValidator(eregService))

    @BeforeEach
    fun setup() {
        every { eregService.organisasjonsnummerEksisterer(any()) } returns true
    }

    @ParameterizedTest
    @MethodSource("validCombinations")
    fun `should be valid for valid combinations`(dto: ArbeidstakerensLonnDto) {
        val violations = validator.validate(dto)
        violations.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun `should be invalid for invalid combinations`(dto: ArbeidstakerensLonnDto) {
        val violations = validator.validate(dto)
        violations.size shouldBe 1
    }

    @ParameterizedTest
    @MethodSource("invalidCombinationsWithNestedValidation")
    fun `should be invalid for invalid combinations with nested validation`(dto: ArbeidstakerensLonnDto) {
        val violations = validator.validate(dto)
        violations.size shouldBe 1
    }

    fun validCombinations(): Stream<Arguments> = listOf(
        // Arbeidsgiver betaler all lønn - virksomheter må være null
        arbeidstakerensLonnDtoMedDefaultVerdier().copy(
            arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = true,
            virksomheterSomUtbetalerLonnOgNaturalytelser = null
        ),
        // Arbeidsgiver betaler ikke all lønn - virksomheter må være satt
        arbeidstakerensLonnDtoMedDefaultVerdier().copy(
            arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = false,
            virksomheterSomUtbetalerLonnOgNaturalytelser = norskeOgUtenlandskeVirksomheterMedDefaultVerdier()
        )
    ).map { Arguments.of(it) }.stream()

    fun invalidCombinations(): Stream<Arguments> = listOf(
        // Arbeidsgiver betaler all lønn, men virksomheter er satt
        arbeidstakerensLonnDtoMedDefaultVerdier().copy(
            arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = true,
            virksomheterSomUtbetalerLonnOgNaturalytelser = norskeOgUtenlandskeVirksomheterMedDefaultVerdier()
        ),
        // Arbeidsgiver betaler ikke all lønn, men virksomheter er null
        arbeidstakerensLonnDtoMedDefaultVerdier().copy(
            arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = false,
            virksomheterSomUtbetalerLonnOgNaturalytelser = null
        )
    ).map { Arguments.of(it) }.stream()

    fun invalidCombinationsWithNestedValidation(): Stream<Arguments> = listOf(
        // Arbeidsgiver betaler ikke all lønn, men norsk virksomhet har ugyldig orgnr (nested validering)
        arbeidstakerensLonnDtoMedDefaultVerdier().copy(
            arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = false,
            virksomheterSomUtbetalerLonnOgNaturalytelser = norskeOgUtenlandskeVirksomheterMedDefaultVerdier().copy(
                norskeVirksomheter = listOf(
                    norskVirksomhetMedDefaultVerdier().copy(
                        organisasjonsnummer = "ugyldig-orgnr"
                    )
                )
            )
        )
    ).map { Arguments.of(it) }.stream()
}
