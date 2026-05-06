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
    @ValueSource(strings = ["3456,78", "100,00", "0,50"])
    fun `erGyldigBelop should accept valid formats`(belop: String) {
        SkatteforholdOgInntektValidator.erGyldigBelop(belop) shouldBe true
    }

    @ParameterizedTest
    @ValueSource(strings = ["3456.78", "-100", "100,", "100,5", "100,123", ",78", "1 000", "100", ""])
    fun `erGyldigBelop should reject invalid formats`(belop: String) {
        SkatteforholdOgInntektValidator.erGyldigBelop(belop) shouldBe false
    }

    fun validCombinations(): Stream<Arguments> = listOf(
        // Mottar ikke pengestøtte - alle felt kan være null
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = false,
        ),
        // Mottar pengestøtte - alle påkrevde felt er satt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = "SE",
            pengestotteSomMottasFraAndreLandBelop = "5000,50",
            pengestotteSomMottasFraAndreLandBeskrivelse = "Studentstøtte fra Sverige"
        ),
        // Lønn + utenlandsk virksomhet + gyldig beløp
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = mapOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET to false, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET to true),
            hvilkeTyperInntektHarDu = mapOf(InntektType.LOENN to true, InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET to false),
            inntekt = "50000,00",
        ),
        // Lønn + kun norsk virksomhet + skatteplikt=NEI + gyldig beløp
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            erSkattepliktigTilNorgeIHeleutsendingsperioden = false,
            inntektFraNorskEllerUtenlandskVirksomhet = mapOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET to true, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET to false),
            hvilkeTyperInntektHarDu = mapOf(InntektType.LOENN to true, InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET to false),
            inntekt = "40000,00",
        ),
        // Inntekt fra egen virksomhet + gyldig beløp
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = mapOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET to true, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET to false),
            hvilkeTyperInntektHarDu = mapOf(InntektType.LOENN to false, InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET to true),
            inntektFraEgenVirksomhet = "30000,00"
        ),
    ).map { Arguments.of(it) }.stream()

    fun invalidCombinations(): Stream<Arguments> = listOf(
        // Mottar pengestøtte, men landSomUtbetalerPengestotte er null
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = null,
            pengestotteSomMottasFraAndreLandBelop = "5000,00",
            pengestotteSomMottasFraAndreLandBeskrivelse = "Beskrivelse"
        ),
        // Mottar pengestøtte, men landSomUtbetalerPengestotte er blank
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = "   ",
            pengestotteSomMottasFraAndreLandBelop = "5000,00",
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
            pengestotteSomMottasFraAndreLandBelop = "5000.50",
            pengestotteSomMottasFraAndreLandBeskrivelse = "Beskrivelse"
        ),
        // Mottar pengestøtte, men pengestotteSomMottasFraAndreLandBeskrivelse er null
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = "SE",
            pengestotteSomMottasFraAndreLandBelop = "5000,00",
            pengestotteSomMottasFraAndreLandBeskrivelse = null
        ),
        // Mottar pengestøtte, men pengestotteSomMottasFraAndreLandBeskrivelse er blank
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            mottarPengestotteFraAnnetEosLandEllerSveits = true,
            landSomUtbetalerPengestotte = "SE",
            pengestotteSomMottasFraAndreLandBelop = "5000,00",
            pengestotteSomMottasFraAndreLandBeskrivelse = "   "
        ),

        // --- Checkbox-grupper ---
        // Ingen arbeidsinntektkilde valgt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = mapOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET to false, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET to false),
            hvilkeTyperInntektHarDu = null
        ),
        // Ingen inntekttype valgt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = mapOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET to true, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET to false),
            hvilkeTyperInntektHarDu = mapOf(InntektType.LOENN to false, InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET to false)
        ),

        // --- Lønnsinntekt ---
        // Skatteplikt=JA + lønn + kun norsk virksomhet → ugyldig kombinasjon
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            erSkattepliktigTilNorgeIHeleutsendingsperioden = true,
            inntektFraNorskEllerUtenlandskVirksomhet = mapOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET to true, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET to false),
            hvilkeTyperInntektHarDu = mapOf(InntektType.LOENN to true, InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET to false),
        ),
        // Lønn + kun norsk virksomhet + skatteplikt=NEI, men beløp mangler
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            erSkattepliktigTilNorgeIHeleutsendingsperioden = false,
            inntektFraNorskEllerUtenlandskVirksomhet = mapOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET to true, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET to false),
            hvilkeTyperInntektHarDu = mapOf(InntektType.LOENN to true, InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET to false),
            inntekt = null,
        ),
        // Lønn + utenlandsk virksomhet, men inntekt mangler
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = mapOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET to false, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET to true),
            hvilkeTyperInntektHarDu = mapOf(InntektType.LOENN to true, InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET to false),
            inntekt = null,
        ),
        // Lønn + utenlandsk virksomhet, men inntekt har ugyldig format
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = mapOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET to true, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET to true),
            hvilkeTyperInntektHarDu = mapOf(InntektType.LOENN to true, InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET to false),
            inntekt = "50000.00",
        ),
        // Lønn ikke valgt, men inntekt er likevel satt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = mapOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET to false, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET to true),
            hvilkeTyperInntektHarDu = mapOf(InntektType.LOENN to false, InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET to true),
            inntekt = "500,00",
            inntektFraEgenVirksomhet = "300,00",
        ),

        // --- Inntekt fra egen virksomhet ---
        // Egen virksomhet valgt, men inntektFraEgenVirksomhet mangler
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = mapOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET to true, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET to false),
            hvilkeTyperInntektHarDu = mapOf(InntektType.LOENN to false, InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET to true),
            inntektFraEgenVirksomhet = null,
        ),
        // Egen virksomhet valgt men inntektFraEgenVirksomhet har ugyldig format
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = mapOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET to true, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET to false),
            hvilkeTyperInntektHarDu = mapOf(InntektType.LOENN to false, InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET to true),
            inntektFraEgenVirksomhet = "not-a-number"
        ),
        // Egen virksomhet ikke valgt, men inntektFraEgenVirksomhet er likevel satt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            inntektFraNorskEllerUtenlandskVirksomhet = mapOf(ArbeidsinntektKilde.NORSK_VIRKSOMHET to false, ArbeidsinntektKilde.UTENLANDSK_VIRKSOMHET to true),
            hvilkeTyperInntektHarDu = mapOf(InntektType.LOENN to true, InntektType.INNTEKT_FRA_EGEN_VIRKSOMHET to false),
            inntekt = "500,00",
            inntektFraEgenVirksomhet = "300,00",
        ),
    ).map { Arguments.of(it) }.stream()
}
