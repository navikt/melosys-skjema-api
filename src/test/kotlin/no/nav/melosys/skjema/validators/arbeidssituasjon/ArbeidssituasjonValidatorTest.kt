package no.nav.melosys.skjema.validators.arbeidssituasjon

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import java.util.stream.Stream
import no.nav.melosys.skjema.arbeidssituasjonDtoMedDefaultVerdier
import no.nav.melosys.skjema.norskVirksomhetMedDefaultVerdier
import no.nav.melosys.skjema.norskeOgUtenlandskeVirksomheterMedAnsettelsesformMedDefaultVerdier
import no.nav.melosys.skjema.types.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.felles.NorskeOgUtenlandskeVirksomheterMedAnsettelsesform
import no.nav.melosys.skjema.utenlandskVirksomhetMedAnsettelsesformMedDefaultVerdier
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidssituasjonValidatorTest {

    private val validator = ArbeidssituasjonValidator()

    @ParameterizedTest
    @MethodSource("validCombinations")
    fun `should be valid for valid combinations`(dto: ArbeidssituasjonDto) {
        val violations = validator.validate(dto)
        violations.shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidCombinations")
    fun `should be invalid for invalid combinations`(dto: ArbeidssituasjonDto) {
        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
    }

    fun validCombinations(): Stream<Arguments> = listOf(
        // Standard case: har vært i lønnet arbeid, skal ikke jobbe for flere virksomheter
        arbeidssituasjonDtoMedDefaultVerdier().copy(
            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
            skalJobbeForFlereVirksomheter = false,
            virksomheterArbeidstakerJobberForIutsendelsesPeriode = null
        ),
        // Ikke vært i lønnet arbeid, men har aktivitet satt
        arbeidssituasjonDtoMedDefaultVerdier().copy(
            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = false,
            aktivitetIMaanedenFoerUtsendingen = "STUDENT",
            skalJobbeForFlereVirksomheter = false
        ),
        // Skal jobbe for flere virksomheter - kun norske virksomheter
        arbeidssituasjonDtoMedDefaultVerdier().copy(
            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
            skalJobbeForFlereVirksomheter = true,
            virksomheterArbeidstakerJobberForIutsendelsesPeriode = NorskeOgUtenlandskeVirksomheterMedAnsettelsesform(
                norskeVirksomheter = listOf(norskVirksomhetMedDefaultVerdier()),
                utenlandskeVirksomheter = null
            )
        ),
        // Skal jobbe for flere virksomheter - kun utenlandske virksomheter
        arbeidssituasjonDtoMedDefaultVerdier().copy(
            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
            skalJobbeForFlereVirksomheter = true,
            virksomheterArbeidstakerJobberForIutsendelsesPeriode = NorskeOgUtenlandskeVirksomheterMedAnsettelsesform(
                norskeVirksomheter = null,
                utenlandskeVirksomheter = listOf(utenlandskVirksomhetMedAnsettelsesformMedDefaultVerdier())
            )
        ),
        // Skal jobbe for flere virksomheter - både norske og utenlandske
        arbeidssituasjonDtoMedDefaultVerdier().copy(
            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
            skalJobbeForFlereVirksomheter = true,
            virksomheterArbeidstakerJobberForIutsendelsesPeriode = norskeOgUtenlandskeVirksomheterMedAnsettelsesformMedDefaultVerdier()
        )
    ).map { Arguments.of(it) }.stream()

    fun invalidCombinations(): Stream<Arguments> = listOf(
        // Ikke vært i lønnet arbeid, men aktivitet er null
        arbeidssituasjonDtoMedDefaultVerdier().copy(
            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = false,
            aktivitetIMaanedenFoerUtsendingen = null,
            skalJobbeForFlereVirksomheter = false
        ),
        // Ikke vært i lønnet arbeid, men aktivitet er blank
        arbeidssituasjonDtoMedDefaultVerdier().copy(
            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = false,
            aktivitetIMaanedenFoerUtsendingen = "",
            skalJobbeForFlereVirksomheter = false
        ),
        // Ikke vært i lønnet arbeid, men aktivitet er kun whitespace
        arbeidssituasjonDtoMedDefaultVerdier().copy(
            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = false,
            aktivitetIMaanedenFoerUtsendingen = "   ",
            skalJobbeForFlereVirksomheter = false
        ),
        // Skal jobbe for flere virksomheter, men virksomheter er null
        arbeidssituasjonDtoMedDefaultVerdier().copy(
            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
            skalJobbeForFlereVirksomheter = true,
            virksomheterArbeidstakerJobberForIutsendelsesPeriode = null
        ),
        // Skal jobbe for flere virksomheter, men begge lister er null
        arbeidssituasjonDtoMedDefaultVerdier().copy(
            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
            skalJobbeForFlereVirksomheter = true,
            virksomheterArbeidstakerJobberForIutsendelsesPeriode = NorskeOgUtenlandskeVirksomheterMedAnsettelsesform(
                norskeVirksomheter = null,
                utenlandskeVirksomheter = null
            )
        ),
        // Skal jobbe for flere virksomheter, men begge lister er tomme
        arbeidssituasjonDtoMedDefaultVerdier().copy(
            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = true,
            skalJobbeForFlereVirksomheter = true,
            virksomheterArbeidstakerJobberForIutsendelsesPeriode = NorskeOgUtenlandskeVirksomheterMedAnsettelsesform(
                norskeVirksomheter = emptyList(),
                utenlandskeVirksomheter = emptyList()
            )
        )
    ).map { Arguments.of(it) }.stream()
}
