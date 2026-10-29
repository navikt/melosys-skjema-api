package no.nav.melosys.skjema.validators.felles

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.util.stream.Stream
import no.nav.melosys.skjema.types.felles.UtenlandskVirksomhet
import no.nav.melosys.skjema.utenlandskVirksomhetMedAnsettelsesformMedDefaultVerdier
import no.nav.melosys.skjema.utenlandskVirksomhetMedDefaultVerdier
import no.nav.melosys.skjema.validators.FELT_ER_PAAKREVD
import no.nav.melosys.skjema.validators.Violation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtenlandskVirksomhetValidatorTest {

    @ParameterizedTest
    @MethodSource("validVirksomheter")
    fun `gyldig utenlandsk virksomhet gir ingen violations`(virksomhet: UtenlandskVirksomhet) {
        UtenlandskVirksomhetValidator.validate(listOf(virksomhet)).shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("manglendePaakrevdeFelter")
    fun `påkrevd felt som mangler eller er blankt gir violation`(virksomhet: UtenlandskVirksomhet, felt: String) {
        UtenlandskVirksomhetValidator.validate(listOf(virksomhet)) shouldBe listOf(
            Violation(field = felt, translationKey = FELT_ER_PAAKREVD)
        )
    }

    @Test
    fun `organisasjonsnummer lengre enn 25 tegn gir violation`() {
        val virksomhet = utenlandskVirksomhetMedDefaultVerdier().copy(organisasjonsnummer = "A".repeat(26))

        UtenlandskVirksomhetValidator.validate(listOf(virksomhet)) shouldBe listOf(
            Violation(
                field = "organisasjonsnummer",
                translationKey = "fellesTranslation.utenlandskOrganisasjonsnummerErForLangt"
            )
        )
    }

    @Test
    fun `validerer alle virksomhetene i lista`() {
        val virksomheter = listOf(
            utenlandskVirksomhetMedAnsettelsesformMedDefaultVerdier(),
            utenlandskVirksomhetMedAnsettelsesformMedDefaultVerdier().copy(byStedsnavn = null),
        )

        UtenlandskVirksomhetValidator.validate(virksomheter) shouldBe listOf(
            Violation(field = "byStedsnavn", translationKey = FELT_ER_PAAKREVD)
        )
    }

    @Test
    fun `ingen virksomheter gir ingen violations`() {
        UtenlandskVirksomhetValidator.validate(null).shouldBeEmpty()
        UtenlandskVirksomhetValidator.validate(emptyList()).shouldBeEmpty()
    }

    fun validVirksomheter(): Stream<Arguments> = listOf(
        utenlandskVirksomhetMedDefaultVerdier(),
        utenlandskVirksomhetMedDefaultVerdier().copy(organisasjonsnummer = null, bygning = null, region = null),
        utenlandskVirksomhetMedDefaultVerdier().copy(organisasjonsnummer = ""),
        utenlandskVirksomhetMedDefaultVerdier().copy(organisasjonsnummer = "A".repeat(25)),
    ).map { Arguments.of(it) }.stream()

    fun manglendePaakrevdeFelter(): Stream<Arguments> = listOf(
        utenlandskVirksomhetMedDefaultVerdier().copy(navn = " ") to "navn",
        utenlandskVirksomhetMedDefaultVerdier().copy(vegnavnOgHusnummer = "") to "vegnavnOgHusnummer",
        utenlandskVirksomhetMedDefaultVerdier().copy(postkode = null) to "postkode",
        utenlandskVirksomhetMedDefaultVerdier().copy(postkode = "  ") to "postkode",
        utenlandskVirksomhetMedDefaultVerdier().copy(byStedsnavn = null) to "byStedsnavn",
        utenlandskVirksomhetMedDefaultVerdier().copy(byStedsnavn = "") to "byStedsnavn",
        utenlandskVirksomhetMedDefaultVerdier().copy(land = "") to "land",
    ).map { (virksomhet, felt) -> Arguments.of(virksomhet, felt) }.stream()
}
