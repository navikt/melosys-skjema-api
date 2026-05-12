package no.nav.melosys.skjema.validators.skatteforholdoginntekt

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.util.stream.Stream
import no.nav.melosys.skjema.skatteforholdOgInntektDtoMedDefaultVerdier
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsinntektKilde
import no.nav.melosys.skjema.types.utsendtarbeidstaker.InntektType
import no.nav.melosys.skjema.types.utsendtarbeidstaker.SkatteforholdOgInntektDto
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

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

    @ParameterizedTest
    @ValueSource(strings = ["1", "100", "3456", "1234567"])
    fun `erGyldigBelop should accept valid formats`(belop: String) {
        SkatteforholdOgInntektValidator.erGyldigBelop(belop) shouldBe true
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "-100", "3456.78", "100,00", "1 000", "", "abc", "12.5"])
    fun `erGyldigBelop should reject invalid formats`(belop: String) {
        SkatteforholdOgInntektValidator.erGyldigBelop(belop) shouldBe false
    }

    fun validCombinations(): Stream<Arguments> = listOf(
        // Mottar ikke pengestøtte, gyldige inntektvalg
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = false,
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = setOf(InntektType.LOENN),
            inntekt = null, // skattepliktig + kun norsk → inntekt ikke tillatt
        ),
        // Mottar pengestøtte - alle påkrevde felt er satt, gyldige inntektvalg
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = "SE",
            pengestotteSomMottasFraAndreLandBelop = "5000",
            pengestotteSomMottasFraAndreLandBeskrivelse = "Studentstøtte fra Sverige",
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = setOf(InntektType.LOENN),
            inntekt = null, // skattepliktig + kun norsk → inntekt ikke tillatt
        ),
        // Lønn + utenlandsk virksomhet + gyldig beløp
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = setOf(InntektType.LOENN),
            inntekt = "50000",
        ),
        // Lønn + kun norsk virksomhet + skatteplikt=NEI + gyldig beløp
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            erSkattepliktigTilNorgeIHeleutsendingsperioden = false,
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = setOf(InntektType.LOENN),
            inntekt = "40000",
        ),
        // Inntekt fra egen virksomhet + gyldig beløp
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = setOf(InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET),
            inntektFraEgenVirksomhet = "30000"
        ),
        // Skatteplikt=JA + lønn + kun norsk virksomhet, uten beløp → gyldig
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            erSkattepliktigTilNorgeIHeleutsendingsperioden = true,
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = setOf(InntektType.LOENN),
            inntekt = null,
        ),
        // Skatteplikt=JA + lønn + egen virksomhet + kun norsk virksomhet → lønn skjult, kun egen virksomhet beløp
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            erSkattepliktigTilNorgeIHeleutsendingsperioden = true,
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = setOf(InntektType.LOENN, InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET),
            inntekt = null,
            inntektFraEgenVirksomhet = "45678",
        ),
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
        // Mottar pengestøtte, men pengestotteSomMottasFraAndreLandBelop har ugyldig format
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = "SE",
            pengestotteSomMottasFraAndreLandBelop = "0",
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

        // --- Checkbox-grupper ---
        // Ingen arbeidsinntektkilde valgt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = emptySet(),
            hvilkeTyperInntektHarDu = null
        ),
        // Ingen inntekttype valgt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = emptySet()
        ),

        // --- Lønnsinntekt ---
        // Skatteplikt=JA + lønn + kun norsk virksomhet, men inntekt er oppgitt → ugyldig
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            erSkattepliktigTilNorgeIHeleutsendingsperioden = true,
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = setOf(InntektType.LOENN),
            inntekt = "50000",
        ),
        // Lønn + kun norsk virksomhet + skatteplikt=NEI, men beløp mangler
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            erSkattepliktigTilNorgeIHeleutsendingsperioden = false,
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = setOf(InntektType.LOENN),
            inntekt = null,
        ),
        // Lønn + utenlandsk virksomhet, men inntekt mangler
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = setOf(InntektType.LOENN),
            inntekt = null,
        ),
        // Lønn + utenlandsk virksomhet, men inntekt har ugyldig format
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = setOf(InntektType.LOENN),
            inntekt = "50000.00",
        ),
        // Lønn ikke valgt, men inntekt er likevel satt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = setOf(InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET),
            inntekt = "500",
            inntektFraEgenVirksomhet = "300",
        ),

        // --- Inntekt fra egen virksomhet ---
        // Egen virksomhet valgt, men inntektFraEgenVirksomhet mangler
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = setOf(InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET),
            inntektFraEgenVirksomhet = null,
        ),
        // Egen virksomhet valgt men inntektFraEgenVirksomhet har ugyldig format
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = setOf(InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET),
            inntektFraEgenVirksomhet = "not-a-number"
        ),
        // Egen virksomhet ikke valgt, men inntektFraEgenVirksomhet er likevel satt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = setOf(ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET),
            hvilkeTyperInntektHarDu = setOf(InntektType.LOENN),
            inntekt = "500",
            inntektFraEgenVirksomhet = "300",
        ),
    ).map { Arguments.of(it) }.stream()
}
