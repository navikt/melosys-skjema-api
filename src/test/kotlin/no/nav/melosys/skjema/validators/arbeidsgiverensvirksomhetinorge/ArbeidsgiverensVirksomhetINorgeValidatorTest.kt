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

    private fun privat(bemanningsbyraa: Boolean, samlet: ArbeidsgiverensVirksomhetINorgeDto? = null, vanligDrift: Boolean? = null) =
        (samlet ?: ArbeidsgiverensVirksomhetINorgeDto()).copy(
            erArbeidsgiverenBemanningsEllerVikarbyraa = bemanningsbyraa,
            opprettholderArbeidsgiverenVanligDrift = vanligDrift
        )

    @Test
    fun `manglende registerklassifisering er en systemfeil, ikke en brukerfeil`() {
        shouldThrow<IllegalStateException> {
            validator.validate(ArbeidsgiverensVirksomhetINorgeDto(), null, MANGE, IKKE_NUF)
        }
    }

    @Test
    fun `manglende ansattall for privat arbeidsgiver er en systemfeil, ikke en brukerfeil`() {
        shouldThrow<IllegalStateException> {
            validator.validate(privat(bemanningsbyraa = false), erOffentligArbeidsgiver = false, antallAnsatte = null, IKKE_NUF)
        }
    }

    @Test
    fun `manglende NUF-klassifisering for privat arbeidsgiver er en systemfeil, ikke en brukerfeil`() {
        shouldThrow<IllegalStateException> {
            validator.validate(privat(bemanningsbyraa = false), erOffentligArbeidsgiver = false, antallAnsatte = MANGE, null)
        }
    }

    @Test
    fun `offentlig arbeidsgiver trenger ikke ansattall eller NUF-klassifisering`() {
        validator.validate(null, erOffentligArbeidsgiver = true, antallAnsatte = null, null).shouldBeEmpty()
    }

    @Test
    fun `offentlig arbeidsgiver krever ikke virksomhetsseksjonen i det hele tatt`() {
        validator.validate(null, erOffentligArbeidsgiver = true, antallAnsatte = FAA, IKKE_NUF).shouldBeEmpty()
    }

    @Test
    fun `privat arbeidsgiver maa fylle ut virksomhetsseksjonen`() {
        validator.validate(null, erOffentligArbeidsgiver = false, antallAnsatte = MANGE, IKKE_NUF).shouldHaveSize(1)
    }

    @Test
    fun `alle manglende felter om samlet virksomhet rapporteres som paakrevd`() {
        val avvik = validator.validate(privat(bemanningsbyraa = false), erOffentligArbeidsgiver = false, antallAnsatte = FAA, IKKE_NUF)

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

        val avvik = validator.validate(dto, erOffentligArbeidsgiver = false, antallAnsatte = FAA, IKKE_NUF)

        avvik.map { it.field to it.translationKey } shouldContainExactlyInAnyOrder listOf(
            "antallUtsendteArbeidstakere" to "arbeidsgiverensVirksomhetINorgeTranslation.antallMaaVaereNullEllerMer",
            "andelOmsetningINorge" to "arbeidsgiverensVirksomhetINorgeTranslation.andelMaaVaereMellom0Og100",
            "andelOppdragUtfortINorge" to "arbeidsgiverensVirksomhetINorgeTranslation.andelMaaVaereMellom0Og100"
        )
    }

    @Test
    fun `20 eller flere ansatte uten bemanningsbyraa skal ikke oppgi samlet virksomhet`() {
        val avvik = validator.validate(
            privat(bemanningsbyraa = false, samletVirksomhet.copy(antallUtsendteArbeidstakere = null), vanligDrift = true),
            erOffentligArbeidsgiver = false,
            antallAnsatte = GRENSE,
            IKKE_NUF
        )

        avvik shouldHaveSize 5
        avvik.map { it.translationKey }.toSet() shouldBe
            setOf("arbeidsgiverensVirksomhetINorgeTranslation.skalIkkeOppgiSamletVirksomhet")
    }

    @Test
    fun `vanlig drift avvises naar samlet virksomhet skal oppgis`() {
        listOf(
            Triple(FAA, false, IKKE_NUF),
            Triple(MANGE, true, IKKE_NUF),
            Triple(MANGE, false, NUF)
        ).forEach { (antallAnsatte, bemanningsbyraa, erNuf) ->
            val avvik = validator.validate(
                privat(bemanningsbyraa, samletVirksomhet, vanligDrift = false),
                erOffentligArbeidsgiver = false,
                antallAnsatte = antallAnsatte,
                erNuf
            )

            avvik.map { it.field to it.translationKey } shouldContainExactlyInAnyOrder listOf(
                "opprettholderArbeidsgiverenVanligDrift" to
                    "arbeidsgiverensVirksomhetINorgeTranslation.skalIkkeOppgiVanligDrift"
            )
        }
    }

    @Test
    fun `offentlig arbeidsgiver skal ikke oppgi samlet virksomhet`() {
        validator.validate(samletVirksomhet, erOffentligArbeidsgiver = true, antallAnsatte = FAA, NUF) shouldHaveSize 6
    }

    @Test
    fun `skalOppgiSamletVirksomhet folger terskel, bemanningsbyraa og NUF`() {
        ArbeidsgiverensVirksomhetINorgeValidator.skalOppgiSamletVirksomhet(0, null, IKKE_NUF) shouldBe true
        ArbeidsgiverensVirksomhetINorgeValidator.skalOppgiSamletVirksomhet(19, false, IKKE_NUF) shouldBe true
        ArbeidsgiverensVirksomhetINorgeValidator.skalOppgiSamletVirksomhet(20, false, IKKE_NUF) shouldBe false
        ArbeidsgiverensVirksomhetINorgeValidator.skalOppgiSamletVirksomhet(20, null, IKKE_NUF) shouldBe false
        ArbeidsgiverensVirksomhetINorgeValidator.skalOppgiSamletVirksomhet(20, true, IKKE_NUF) shouldBe true
        ArbeidsgiverensVirksomhetINorgeValidator.skalOppgiSamletVirksomhet(20, false, NUF) shouldBe true
        ArbeidsgiverensVirksomhetINorgeValidator.skalOppgiSamletVirksomhet(MANGE, null, NUF) shouldBe true
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("gyldigeKombinasjoner")
    fun `gyldige kombinasjoner gir ingen avvik`(
        dto: ArbeidsgiverensVirksomhetINorgeDto,
        erOffentligArbeidsgiver: Boolean,
        antallAnsatte: Int,
        beskrivelse: String,
        erNuf: Boolean
    ) {
        validator.validate(dto, erOffentligArbeidsgiver, antallAnsatte, erNuf).shouldBeEmpty()
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("ugyldigeKombinasjoner")
    fun `ugyldige kombinasjoner gir avvik`(
        dto: ArbeidsgiverensVirksomhetINorgeDto,
        erOffentligArbeidsgiver: Boolean,
        antallAnsatte: Int,
        beskrivelse: String,
        erNuf: Boolean
    ) {
        validator.validate(dto, erOffentligArbeidsgiver, antallAnsatte, erNuf).shouldHaveSize(1)
    }

    fun gyldigeKombinasjoner(): Stream<Arguments> = Stream.of(
        Arguments.of(ArbeidsgiverensVirksomhetINorgeDto(), true, FAA, "offentlig uten oppfoelgingssvar", IKKE_NUF),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(
                erArbeidsgiverenBemanningsEllerVikarbyraa = false,
                opprettholderArbeidsgiverenVanligDrift = true
            ),
            false,
            MANGE,
            "privat med 20 eller flere ansatte, ikke bemanningsbyraa",
            IKKE_NUF
        ),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(
                erArbeidsgiverenBemanningsEllerVikarbyraa = false,
                opprettholderArbeidsgiverenVanligDrift = false
            ),
            false,
            GRENSE,
            "privat med akkurat 20 ansatte, begge svar nei",
            IKKE_NUF
        ),
        Arguments.of(privat(bemanningsbyraa = false, samletVirksomhet), false, FAA, "privat med faerre enn 20 ansatte uten vanlig drift", IKKE_NUF),
        Arguments.of(privat(bemanningsbyraa = true, samletVirksomhet), false, MANGE, "bemanningsbyraa med 20 eller flere ansatte uten vanlig drift", IKKE_NUF),
        Arguments.of(privat(bemanningsbyraa = false, samletVirksomhet), false, 0, "privat uten registrerte ansatte og vanlig drift", IKKE_NUF),
        Arguments.of(privat(bemanningsbyraa = false, samletVirksomhet), false, MANGE, "NUF med 20 eller flere ansatte uten vanlig drift", NUF),
        Arguments.of(ArbeidsgiverensVirksomhetINorgeDto(), true, MANGE, "offentlig uten oppfoelgingssvar selv om NUF-flagget er satt", NUF)
    )

    fun ugyldigeKombinasjoner(): Stream<Arguments> = Stream.of(
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(erArbeidsgiverenBemanningsEllerVikarbyraa = true),
            true,
            FAA,
            "offentlig som har svart om bemanningsbyraa",
            IKKE_NUF
        ),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(opprettholderArbeidsgiverenVanligDrift = true),
            true,
            FAA,
            "offentlig som har svart om vanlig drift",
            IKKE_NUF
        ),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(
                erArbeidsgiverenBemanningsEllerVikarbyraa = true,
                opprettholderArbeidsgiverenVanligDrift = true
            ),
            true,
            FAA,
            "offentlig som har svart paa begge",
            IKKE_NUF
        ),
        Arguments.of(ArbeidsgiverensVirksomhetINorgeDto(), false, MANGE, "privat uten oppfoelgingssvar", IKKE_NUF),
        Arguments.of(
            privat(bemanningsbyraa = false),
            false,
            MANGE,
            "privat med mange ansatte som mangler svar om vanlig drift",
            IKKE_NUF
        ),
        Arguments.of(
            ArbeidsgiverensVirksomhetINorgeDto(opprettholderArbeidsgiverenVanligDrift = true),
            false,
            MANGE,
            "privat som mangler svar om bemanningsbyraa",
            IKKE_NUF
        ),
        Arguments.of(
            privat(bemanningsbyraa = true, samletVirksomhet.copy(andelOppdragskontrakterInngattINorge = null)),
            false,
            MANGE,
            "bemanningsbyraa som mangler ett felt om samlet virksomhet",
            IKKE_NUF
        ),
        Arguments.of(
            privat(bemanningsbyraa = false, samletVirksomhet, vanligDrift = true),
            false,
            FAA,
            "privat med faerre enn 20 ansatte skal ikke svare om vanlig drift",
            IKKE_NUF
        ),
        Arguments.of(
            privat(bemanningsbyraa = true, samletVirksomhet, vanligDrift = false),
            false,
            MANGE,
            "bemanningsbyraa skal ikke svare om vanlig drift",
            IKKE_NUF
        ),
        Arguments.of(
            privat(bemanningsbyraa = false, samletVirksomhet.copy(andelOmsetningINorge = null)),
            false,
            MANGE,
            "NUF med 20 eller flere ansatte som mangler ett felt om samlet virksomhet",
            NUF
        ),
        Arguments.of(
            privat(bemanningsbyraa = false, samletVirksomhet, vanligDrift = true),
            false,
            MANGE,
            "NUF skal ikke svare om vanlig drift",
            NUF
        )
    )

    private companion object {
        const val FAA = 5
        const val GRENSE = 20
        const val MANGE = 50
        const val NUF = true
        const val IKKE_NUF = false
    }
}
