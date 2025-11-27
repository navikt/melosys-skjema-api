package no.nav.melosys.skjema.controller.validators.arbeidsgiverensvirksomhetinorge

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.stream.Stream
import no.nav.melosys.skjema.arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier
import no.nav.melosys.skjema.controller.validators.BaseValidatorTest
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidsgiverensVirksomhetINorgeValidatorTest: BaseValidatorTest() {

    @Test
    fun `ArbeidsgiverensVirksomhetINorgeDto should be annotated with GyldigArbeidsgiverensVirksomhet`() {
        val annotation = ArbeidsgiverensVirksomhetINorgeDto::class.java.getAnnotation(GyldigArbeidsgiverensVirksomhet::class.java)
        annotation.shouldNotBeNull()
    }

    @ParameterizedTest
    @MethodSource("validCombinations")
    fun `should be valid for valid combinations`(dto: ArbeidsgiverensVirksomhetINorgeDto) {
        val violations = validator.validate(dto)
        violations.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun `should be invalid for invalid combinations`(dto: ArbeidsgiverensVirksomhetINorgeDto) {
        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
        violations.first().message.shouldBe("Ugyldig arbeidsgiverens virksomhet")
    }

    fun validCombinations(): Stream<Arguments> = listOf(
        arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier().copy(
            erArbeidsgiverenOffentligVirksomhet = true,
            erArbeidsgiverenBemanningsEllerVikarbyraa = null,
            opprettholderArbeidsgiverenVanligDrift = null
        ),
        arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier().copy(
            erArbeidsgiverenOffentligVirksomhet = false,
            erArbeidsgiverenBemanningsEllerVikarbyraa = true,
            opprettholderArbeidsgiverenVanligDrift = true
        ),
        arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier().copy(
            erArbeidsgiverenOffentligVirksomhet = false,
            erArbeidsgiverenBemanningsEllerVikarbyraa = false,
            opprettholderArbeidsgiverenVanligDrift = false
        )
    ).map { Arguments.of(it) }.stream()

    fun invalidCombinations(): Stream<Arguments> = listOf(
        arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier().copy(
            erArbeidsgiverenOffentligVirksomhet = true,
            erArbeidsgiverenBemanningsEllerVikarbyraa = true,
            opprettholderArbeidsgiverenVanligDrift = null
        ),
        arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier().copy(
            erArbeidsgiverenOffentligVirksomhet = true,
            erArbeidsgiverenBemanningsEllerVikarbyraa = null,
            opprettholderArbeidsgiverenVanligDrift = true
        ),
        arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier().copy(
            erArbeidsgiverenOffentligVirksomhet = true,
            erArbeidsgiverenBemanningsEllerVikarbyraa = true,
            opprettholderArbeidsgiverenVanligDrift = true
        ),
        arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier().copy(
            erArbeidsgiverenOffentligVirksomhet = false,
            erArbeidsgiverenBemanningsEllerVikarbyraa = null,
            opprettholderArbeidsgiverenVanligDrift = null
        ),
        arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier().copy(
            erArbeidsgiverenOffentligVirksomhet = false,
            erArbeidsgiverenBemanningsEllerVikarbyraa = true,
            opprettholderArbeidsgiverenVanligDrift = null
        ),
        arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier().copy(
            erArbeidsgiverenOffentligVirksomhet = false,
            erArbeidsgiverenBemanningsEllerVikarbyraa = null,
            opprettholderArbeidsgiverenVanligDrift = true
        )
    ).map { Arguments.of(it) }.stream()
}
