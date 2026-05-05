package no.nav.melosys.skjema.validators.skatteforholdoginntekt

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.util.stream.Stream
import no.nav.melosys.skjema.skatteforholdOgInntektDtoMedDefaultVerdier
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
            arbeidsinntektFraNorskEllerUtenlandskVirksomhet = mapOf("NORSK_VIRKSOMHET" to false, "UTENLANDSK_VIRKSOMHET" to true),
            hvilkeTyperInntektHarDu = mapOf("LOENN" to true, "INNTEKT_FRA_EGEN_VIRKSOMHET" to false),
            inntekterFraUtenlandskVirksomhet = "50000,00",
        ),
        // Lønn + kun norsk virksomhet + skatteplikt=NEI + gyldig beløp
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            erSkattepliktigTilNorgeIHeleutsendingsperioden = false,
            arbeidsinntektFraNorskEllerUtenlandskVirksomhet = mapOf("NORSK_VIRKSOMHET" to true, "UTENLANDSK_VIRKSOMHET" to false),
            hvilkeTyperInntektHarDu = mapOf("LOENN" to true, "INNTEKT_FRA_EGEN_VIRKSOMHET" to false),
            inntekterFraUtenlandskVirksomhet = "40000,00",
        ),
        // Inntekt fra egen virksomhet + gyldig beløp
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            arbeidsinntektFraNorskEllerUtenlandskVirksomhet = mapOf("NORSK_VIRKSOMHET" to true, "UTENLANDSK_VIRKSOMHET" to false),
            hvilkeTyperInntektHarDu = mapOf("LOENN" to false, "INNTEKT_FRA_EGEN_VIRKSOMHET" to true),
            inntekterFraEgenVirksomhet = "30000,00"
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
            arbeidsinntektFraNorskEllerUtenlandskVirksomhet = mapOf("NORSK_VIRKSOMHET" to false, "UTENLANDSK_VIRKSOMHET" to false),
            hvilkeTyperInntektHarDu = null
        ),
        // Ingen inntekttype valgt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            arbeidsinntektFraNorskEllerUtenlandskVirksomhet = mapOf("NORSK_VIRKSOMHET" to true, "UTENLANDSK_VIRKSOMHET" to false),
            hvilkeTyperInntektHarDu = mapOf("LOENN" to false, "INNTEKT_FRA_EGEN_VIRKSOMHET" to false)
        ),

        // --- Lønnsinntekt ---
        // Skatteplikt=JA + lønn + kun norsk virksomhet → ugyldig kombinasjon
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            erSkattepliktigTilNorgeIHeleutsendingsperioden = true,
            arbeidsinntektFraNorskEllerUtenlandskVirksomhet = mapOf("NORSK_VIRKSOMHET" to true, "UTENLANDSK_VIRKSOMHET" to false),
            hvilkeTyperInntektHarDu = mapOf("LOENN" to true, "INNTEKT_FRA_EGEN_VIRKSOMHET" to false),
        ),
        // Lønn + kun norsk virksomhet + skatteplikt=NEI, men beløp mangler
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            erSkattepliktigTilNorgeIHeleutsendingsperioden = false,
            arbeidsinntektFraNorskEllerUtenlandskVirksomhet = mapOf("NORSK_VIRKSOMHET" to true, "UTENLANDSK_VIRKSOMHET" to false),
            hvilkeTyperInntektHarDu = mapOf("LOENN" to true, "INNTEKT_FRA_EGEN_VIRKSOMHET" to false),
            inntekterFraUtenlandskVirksomhet = null,
        ),
        // Lønn + utenlandsk virksomhet, men inntekterFraUtenlandskVirksomhet mangler
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            arbeidsinntektFraNorskEllerUtenlandskVirksomhet = mapOf("NORSK_VIRKSOMHET" to false, "UTENLANDSK_VIRKSOMHET" to true),
            hvilkeTyperInntektHarDu = mapOf("LOENN" to true, "INNTEKT_FRA_EGEN_VIRKSOMHET" to false),
            inntekterFraUtenlandskVirksomhet = null,
        ),
        // Lønn + utenlandsk virksomhet, men inntekterFraUtenlandskVirksomhet har ugyldig format
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            arbeidsinntektFraNorskEllerUtenlandskVirksomhet = mapOf("NORSK_VIRKSOMHET" to true, "UTENLANDSK_VIRKSOMHET" to true),
            hvilkeTyperInntektHarDu = mapOf("LOENN" to true, "INNTEKT_FRA_EGEN_VIRKSOMHET" to false),
            inntekterFraUtenlandskVirksomhet = "50000.00",
        ),
        // Lønn ikke valgt, men inntekterFraUtenlandskVirksomhet er likevel satt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            arbeidsinntektFraNorskEllerUtenlandskVirksomhet = mapOf("NORSK_VIRKSOMHET" to false, "UTENLANDSK_VIRKSOMHET" to true),
            hvilkeTyperInntektHarDu = mapOf("LOENN" to false, "INNTEKT_FRA_EGEN_VIRKSOMHET" to true),
            inntekterFraUtenlandskVirksomhet = "500,00",
            inntekterFraEgenVirksomhet = "300,00",
        ),

        // --- Inntekt fra egen virksomhet ---
        // Egen virksomhet valgt, men inntekterFraEgenVirksomhet mangler
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            arbeidsinntektFraNorskEllerUtenlandskVirksomhet = mapOf("NORSK_VIRKSOMHET" to true, "UTENLANDSK_VIRKSOMHET" to false),
            hvilkeTyperInntektHarDu = mapOf("LOENN" to false, "INNTEKT_FRA_EGEN_VIRKSOMHET" to true),
            inntekterFraEgenVirksomhet = null,
        ),
        // Egen virksomhet valgt men inntekterFraEgenVirksomhet har ugyldig format
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            arbeidsinntektFraNorskEllerUtenlandskVirksomhet = mapOf("NORSK_VIRKSOMHET" to true, "UTENLANDSK_VIRKSOMHET" to false),
            hvilkeTyperInntektHarDu = mapOf("LOENN" to false, "INNTEKT_FRA_EGEN_VIRKSOMHET" to true),
            inntekterFraEgenVirksomhet = "not-a-number"
        ),
        // Egen virksomhet ikke valgt, men inntekterFraEgenVirksomhet er likevel satt
        skatteforholdOgInntektDtoMedDefaultVerdier().copy(
            arbeidsinntektFraNorskEllerUtenlandskVirksomhet = mapOf("NORSK_VIRKSOMHET" to false, "UTENLANDSK_VIRKSOMHET" to true),
            hvilkeTyperInntektHarDu = mapOf("LOENN" to true, "INNTEKT_FRA_EGEN_VIRKSOMHET" to false),
            inntekterFraUtenlandskVirksomhet = "500,00",
            inntekterFraEgenVirksomhet = "300,00",
        ),
    ).map { Arguments.of(it) }.stream()
}
