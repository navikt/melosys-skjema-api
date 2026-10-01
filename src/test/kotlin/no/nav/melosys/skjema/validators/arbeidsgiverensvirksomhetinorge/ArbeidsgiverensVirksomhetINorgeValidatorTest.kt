package no.nav.melosys.skjema.validators.arbeidsgiverensvirksomhetinorge

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.util.stream.Stream
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.validators.FELT_ER_PAAKREVD
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidsgiverensVirksomhetINorgeValidatorTest {

    private val validator = ArbeidsgiverensVirksomhetINorgeValidator()

    private val samletVirksomhet = ArbeidsgiverensVirksomhetINorgeDto(
        antallAdministrativtAnsatte = 2,
        antallUtsendteArbeidstakere = 3,
        andelAnsatteRekruttertINorge = 0,
        andelOmsetningINorge = 100,
        andelOppdragUtfortINorge = 50,
        andelOppdragskontrakterInngattINorge = 75
    )

    private fun privat(bemanningsbyraa: Boolean, samlet: ArbeidsgiverensVirksomhetINorgeDto? = null) =
        (samlet ?: ArbeidsgiverensVirksomhetINorgeDto()).copy(
            erArbeidsgiverenBemanningsEllerVikarbyraa = bemanningsbyraa,
            opprettholderArbeidsgiverenVanligDrift = true
        )

    @Test
    fun `manglende registerklassifisering er en systemfeil, ikke en brukerfeil`() {
        shouldThrow<IllegalStateException> {
            validator.validate(ArbeidsgiverensVirksomhetINorgeDto(), null, MANGE)
        }
    }

    @Test
    fun `manglende ansattall for privat arbeidsgiver er en systemfeil, ikke en brukerfeil`() {
        shouldThrow<IllegalStateException> {
            validator.validate(privat(bemanningsbyraa = false), erOffentligArbeidsgiver = false, antallAnsatte = null)
        }
    }

    @Test
    fun `offentlig arbeidsgiver trenger ikke ansattall`() {
        validator.validate(null, erOffentligArbeidsgiver = true, antallAnsatte = null).shouldBeEmpty()
    }

    @Test
    fun `offentlig arbeidsgiver krever ikke virksomhetsseksjonen i det hele tatt`() {
        validator.validate(null, erOffentligArbeidsgiver = true, antallAnsatte = FAA).shouldBeEmpty()
    }

    @Test
    fun `privat arbeidsgiver maa fylle ut virksomhetsseksjonen`() {
        validator.validate(null, erOffentligArbeidsgiver = false, antallAnsatte = MANGE).shouldHaveSize(1)
    }

    @Test
    fun `alle manglende felter om samlet virksomhet rapporteres som paakrevd`() {
        val avvik = validator.validate(privat(bemanningsbyraa = false), erOffentligArbeidsgiver = false, antallAnsatte = FAA)

        avvik.map { it.field } shouldContainExactlyInAnyOrder listOf(
            "antallAdministrativtAnsatte",
            "antallUtsendteArbeidstakere",
            "andelAnsatteRekruttertINorge",
            "andelOmsetningINorge",
            "andelOppdragUtfortINorge",
            "andelOppdragskontrakterInngattINorge"
        )
        avvik.map { it.translationKey }.toSet() shouldBe setOf(FELT_ER_PAAKREVD)
    }

    @Test
    fun `negativt antall og andel utenfor 0 til 100 avvises`() {
        val dto = privat(
            bemanningsbyraa = false,
            samletVirksomhet.copy(antallUtsendteArbeidstakere = -1, andelOmsetningINorge = 101, andelOppdragUtfortINorge = -1)
        )

        val avvik = validator.validate(dto, erOffentligArbeidsgiver = false, antallAnsatte = FAA)

        avvik.map { it.field to it.translationKey } shouldContainExactlyInAnyOrder listOf(
            "antallUtsendteArbeidstakere" to "arbeidsgiverensVirksomhetINorgeTranslation.antallMaaVaereNullEllerMer",
            "andelOmsetningINorge" to "arbeidsgiverensVirksomhetINorgeTranslation.andelMaaVaereMellom0Og100",
            "andelOppdragUtfortINorge" to "arbeidsgiverensVirksomhetINorgeTranslation.andelMaaVaereMellom0Og100"
        )
    }

    @Test
    fun `20 eller flere ansatte uten bemanningsbyraa skal ikke oppgi samlet virksomhet`() {
        val avvik = validator.validate(
            privat(bemanningsbyraa = false, samletVirksomhet.copy(antallUtsendteArbeidstakere = null)),
            erOffentligArbeidsgiver = false,
            antallAnsatte = GRENSE
        )

        avvik shouldHaveSize 5
        avvik.map { it.translationKey }.toSet() shouldBe
            setOf("arbeidsgiverensVirksomhetINorgeTranslation.skalIkkeOppgiSamletVirksomhet")
    }

    @Test
    fun `offentlig arbeidsgiver skal ikke oppgi samlet virksomhet`() {
        validator.validate(samletVirksomhet, erOffentligArbeidsgiver = true, antallAnsatte = FAA) shouldHaveSize 6
    }

    @Test
    fun `skalOppgiSamletVirksomhet folger terskel og bemanningsbyraa`() {
        ArbeidsgiverensVirksomhetINorgeValidator.skalOppgiSamletVirksomhet(0, null) shouldBe true
        ArbeidsgiverensVirksomhetINorgeValidator.skalOppgiSamletVirksomhet(19, false) shouldBe true
        ArbeidsgiverensVirksomhetINorgeValidator.skalOppgiSamletVirksomhet(20, false) shouldBe false
        ArbeidsgiverensVirksomhetINorgeValidator.skalOppgiSamletVirksomhet(20, null) shouldBe false
        ArbeidsgiverensVirksomhetINorgeValidator.skalOppgiSamletVirksomhet(20, true) shouldBe true
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("gyldigeKombinasjoner")
    fun `gyldige kombinasjoner gir ingen avvik`(
        dto: ArbeidsgiverensVirksomhetINorgeDto,
        erOffentligArbeidsgiver: Boolean,
        antallAnsatte: Int,
        beskrivelse: String
    ) {
        validator.validate(dto, erOffentligArbeidsgiver, antallAnsatte).shouldBeEmpty()
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("ugyldigeKombinasjoner")
    fun `ugyldige kombinasjoner gir avvik`(
        dto: ArbeidsgiverensVirksomhetINorgeDto,
        erOffentligArbeidsgiver: Boolean,
        antallAnsatte: Int,
        beskrivelse: String
    ) {
        validator.validate(dto, erOffentligArbeidsgiver, antallAnsatte).shouldHaveSize(1)
    }

    fun gyldigeKombinasjoner(): Stream<Arguments> = Stream.of(
        Arguments.of(ArbeidsgiverensVirksomhetINorgeDto(), true, FAA, "offentlig uten oppfoelgingssvar"),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(
                erArbeidsgiverenBemanningsEllerVikarbyraa = false,
                opprettholderArbeidsgiverenVanligDrift = true
            ),
            false,
            MANGE,
            "privat med 20 eller flere ansatte, ikke bemanningsbyraa"
        ),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(
                erArbeidsgiverenBemanningsEllerVikarbyraa = false,
                opprettholderArbeidsgiverenVanligDrift = false
            ),
            false,
            GRENSE,
            "privat med akkurat 20 ansatte, begge svar nei"
        ),
        Arguments.of(privat(bemanningsbyraa = false, samletVirksomhet), false, FAA, "privat med faerre enn 20 ansatte og samlet virksomhet"),
        Arguments.of(privat(bemanningsbyraa = true, samletVirksomhet), false, MANGE, "bemanningsbyraa med 20 eller flere ansatte og samlet virksomhet"),
        Arguments.of(privat(bemanningsbyraa = false, samletVirksomhet), false, 0, "privat uten registrerte ansatte og samlet virksomhet")
    )

    fun ugyldigeKombinasjoner(): Stream<Arguments> = Stream.of(
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(erArbeidsgiverenBemanningsEllerVikarbyraa = true),
            true,
            FAA,
            "offentlig som har svart om bemanningsbyraa"
        ),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(opprettholderArbeidsgiverenVanligDrift = true),
            true,
            FAA,
            "offentlig som har svart om vanlig drift"
        ),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(
                erArbeidsgiverenBemanningsEllerVikarbyraa = true,
                opprettholderArbeidsgiverenVanligDrift = true
            ),
            true,
            FAA,
            "offentlig som har svart paa begge"
        ),
        Arguments.of(ArbeidsgiverensVirksomhetINorgeDto(), false, MANGE, "privat uten oppfoelgingssvar"),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(erArbeidsgiverenBemanningsEllerVikarbyraa = true),
            false,
            MANGE,
            "privat som mangler svar om vanlig drift"
        ),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(opprettholderArbeidsgiverenVanligDrift = true),
            false,
            MANGE,
            "privat som mangler svar om bemanningsbyraa"
        ),
        Arguments.of(
            privat(bemanningsbyraa = true, samletVirksomhet.copy(andelOppdragskontrakterInngattINorge = null)),
            false,
            MANGE,
            "bemanningsbyraa som mangler ett felt om samlet virksomhet"
        )
    )

    private companion object {
        const val FAA = 5
        const val GRENSE = 20
        const val MANGE = 50
    }
}
