package no.nav.melosys.skjema.validators.arbeidsgiverensvirksomhetinorge

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import java.util.stream.Stream
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverensVirksomhetINorgeDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidsgiverensVirksomhetINorgeValidatorTest {

    private val validator = ArbeidsgiverensVirksomhetINorgeValidator()

    @Test
    fun `manglende registerklassifisering er en systemfeil, ikke en brukerfeil`() {
        shouldThrow<IllegalStateException> {
            validator.validate(ArbeidsgiverensVirksomhetINorgeDto(), null)
        }
    }

    @Test
    fun `offentlig arbeidsgiver krever ikke virksomhetsseksjonen i det hele tatt`() {
        validator.validate(null, erOffentligArbeidsgiver = true).shouldBeEmpty()
    }

    @Test
    fun `privat arbeidsgiver maa fylle ut virksomhetsseksjonen`() {
        validator.validate(null, erOffentligArbeidsgiver = false).shouldHaveSize(1)
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("gyldigeKombinasjoner")
    fun `gyldige kombinasjoner gir ingen avvik`(
        dto: ArbeidsgiverensVirksomhetINorgeDto,
        erOffentligArbeidsgiver: Boolean,
        beskrivelse: String
    ) {
        validator.validate(dto, erOffentligArbeidsgiver).shouldBeEmpty()
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("ugyldigeKombinasjoner")
    fun `ugyldige kombinasjoner gir avvik`(
        dto: ArbeidsgiverensVirksomhetINorgeDto,
        erOffentligArbeidsgiver: Boolean,
        beskrivelse: String
    ) {
        validator.validate(dto, erOffentligArbeidsgiver).shouldHaveSize(1)
    }

    fun gyldigeKombinasjoner(): Stream<Arguments> = Stream.of(
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(),
            true,
            "offentlig uten oppfoelgingssvar"
        ),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(
                erArbeidsgiverenBemanningsEllerVikarbyraa = true,
                opprettholderArbeidsgiverenVanligDrift = true
            ),
            false,
            "privat med begge oppfoelgingssvar satt til ja"
        ),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(
                erArbeidsgiverenBemanningsEllerVikarbyraa = false,
                opprettholderArbeidsgiverenVanligDrift = false
            ),
            false,
            "privat med begge oppfoelgingssvar satt til nei"
        )
    )

    fun ugyldigeKombinasjoner(): Stream<Arguments> = Stream.of(
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(erArbeidsgiverenBemanningsEllerVikarbyraa = true),
            true,
            "offentlig som har svart om bemanningsbyraa"
        ),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(opprettholderArbeidsgiverenVanligDrift = true),
            true,
            "offentlig som har svart om vanlig drift"
        ),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(
                erArbeidsgiverenBemanningsEllerVikarbyraa = true,
                opprettholderArbeidsgiverenVanligDrift = true
            ),
            true,
            "offentlig som har svart paa begge"
        ),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(),
            false,
            "privat uten oppfoelgingssvar"
        ),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(erArbeidsgiverenBemanningsEllerVikarbyraa = true),
            false,
            "privat som mangler svar om vanlig drift"
        ),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(opprettholderArbeidsgiverenVanligDrift = true),
            false,
            "privat som mangler svar om bemanningsbyraa"
        )
    )
}
