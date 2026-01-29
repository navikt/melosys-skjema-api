package no.nav.melosys.skjema.validators.arbeidsgiverensvirksomhetinorge

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import java.util.stream.Stream
import no.nav.melosys.skjema.arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidsgiverensVirksomhetINorgeValidatorTest {

    private val validator = ArbeidsgiverensVirksomhetINorgeValidator()

    @ParameterizedTest
    @MethodSource("validCombinations")
    fun `should be valid for valid combinations`(dto: ArbeidsgiverensVirksomhetINorgeDto) {
        validator.validate(dto).shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun `should be invalid for invalid combinations`(dto: ArbeidsgiverensVirksomhetINorgeDto) {
        validator.validate(dto).shouldHaveSize(1)
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
