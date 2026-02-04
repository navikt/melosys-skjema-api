package no.nav.melosys.skjema.validators.familiemedlemmer

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import java.time.LocalDate
import java.util.stream.Stream
import no.nav.melosys.skjema.familiemedlemMedDefaultVerdier
import no.nav.melosys.skjema.korrektSyntetiskFnr
import no.nav.melosys.skjema.types.arbeidstaker.familiemedlemmer.Familiemedlem
import no.nav.melosys.skjema.validators.felles.ErFodselsEllerDNummerValidator
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FamiliemedlemValidatorTest {

    private val validator = FamiliemedlemValidator(
        ErFodselsEllerDNummerValidator(true)
    )

    @ParameterizedTest
    @MethodSource("validCombinations")
    fun `should be valid for valid combinations`(familiemedlem: Familiemedlem) {
        val violations = validator.validate(familiemedlem)
        violations.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun `should be invalid for invalid combinations`(familiemedlem: Familiemedlem, expectedViolationCount: Int) {
        val violations = validator.validate(familiemedlem)
        violations.shouldHaveSize(expectedViolationCount)
    }

    fun validCombinations(): Stream<Arguments> = listOf(
        // Har norsk fødselsnummer
        familiemedlemMedDefaultVerdier().copy(
            harNorskFodselsnummerEllerDnummer = true,
            fodselsnummer = korrektSyntetiskFnr,
            fodselsdato = null
        ),
        // Har ikke norsk fødselsnummer, men har fødselsdato
        familiemedlemMedDefaultVerdier().copy(
            harNorskFodselsnummerEllerDnummer = false,
            fodselsnummer = null,
            fodselsdato = LocalDate.of(2000, 1, 1)
        )
    ).map { Arguments.of(it) }.stream()

    fun invalidCombinations(): Stream<Arguments> = listOf(
        // Fornavn er blank
        Arguments.of(
            familiemedlemMedDefaultVerdier().copy(fornavn = ""),
            1
        ),
        // Etternavn er blank
        Arguments.of(
            familiemedlemMedDefaultVerdier().copy(etternavn = ""),
            1
        ),
        // Har norsk fødselsnummer men fodselsnummer er null
        Arguments.of(
            familiemedlemMedDefaultVerdier().copy(
                harNorskFodselsnummerEllerDnummer = true,
                fodselsnummer = null
            ),
            1
        ),
        // Har norsk fødselsnummer men fodselsnummer er blank (2 violations: FamiliemedlemValidator + ErFodselsEllerDNummer)
        Arguments.of(
            familiemedlemMedDefaultVerdier().copy(
                harNorskFodselsnummerEllerDnummer = true,
                fodselsnummer = ""
            ),
            1
        ),
        // Har ikke norsk fødselsnummer men fødselsdato er null
        Arguments.of(
            familiemedlemMedDefaultVerdier().copy(
                harNorskFodselsnummerEllerDnummer = false,
                fodselsnummer = null,
                fodselsdato = null
            ),
            1
        ),
        // Fornavn og etternavn er blank
        Arguments.of(
            familiemedlemMedDefaultVerdier().copy(
                fornavn = "",
                etternavn = ""
            ),
            1
        )
    ).stream()
}
