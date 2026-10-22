package no.nav.melosys.skjema.validators

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.stream.Stream
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.validators.arbeidsgiverensvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeValidator
import no.nav.melosys.skjema.validators.arbeidssituasjon.ArbeidssituasjonValidator
import no.nav.melosys.skjema.validators.arbeidsstediutlandet.ArbeidsstedIUtlandetValidator
import no.nav.melosys.skjema.validators.arbeidstakerenslonn.ArbeidstakerensLonnValidator
import no.nav.melosys.skjema.validators.familiemedlemmer.FamiliemedlemmerValidator
import no.nav.melosys.skjema.validators.skatteforholdoginntekt.SkatteforholdOgInntektValidator
import no.nav.melosys.skjema.validators.tilleggsopplysninger.TilleggsopplysningerValidator
import no.nav.melosys.skjema.validators.utenlandsoppdraget.UtenlandsoppdragetValidator
import no.nav.melosys.skjema.validators.utsendingsperiodeogland.UtsendingsperiodeOgLandValidator
import no.nav.melosys.skjema.validators.vedlegg.VedleggValgValidator
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UtsendtArbeidstakerSkjemaDataValidatorTest {

    private val arbeidsgiverensVirksomhetValidator = mockk<ArbeidsgiverensVirksomhetINorgeValidator>()
    private val utenlandsoppdragetValidator = mockk<UtenlandsoppdragetValidator>()
    private val utsendingsperiodeOgLandValidator = mockk<UtsendingsperiodeOgLandValidator>()
    private val arbeidstakerensLonnValidator = mockk<ArbeidstakerensLonnValidator>()
    private val arbeidsstedIUtlandetValidator = mockk<ArbeidsstedIUtlandetValidator>()
    private val tilleggsopplysningerValidator = mockk<TilleggsopplysningerValidator>()
    private val arbeidssituasjonValidator = mockk<ArbeidssituasjonValidator>()
    private val skatteforholdOgInntektValidator = mockk<SkatteforholdOgInntektValidator>()
    private val familiemedlemmerValidator = mockk<FamiliemedlemmerValidator>()
    private val vedleggValgValidator = mockk<VedleggValgValidator>()

    private val privatFlyt = UtsendtArbeidstakerFlyt(
        utenlandsoppdragetSkalOppgis = true,
        arbeidsstedSkalOppgis = true,
        forenkletArbeidssted = false,
        forenkletArbeidstakerdel = false,
        lonnOppgisISkatteforhold = false,
    )

    private val validator = UtsendtArbeidstakerSkjemaDataValidator(
        arbeidsgiverensVirksomhetValidator = arbeidsgiverensVirksomhetValidator,
        utenlandsoppdragetValidator = utenlandsoppdragetValidator,
        utsendingsperiodeOgLandValidator = utsendingsperiodeOgLandValidator,
        arbeidstakerensLonnValidator = arbeidstakerensLonnValidator,
        arbeidsstedIUtlandetValidator = arbeidsstedIUtlandetValidator,
        tilleggsopplysningerValidator = tilleggsopplysningerValidator,
        arbeidssituasjonValidator = arbeidssituasjonValidator,
        skatteforholdOgInntektValidator = skatteforholdOgInntektValidator,
        familiemedlemmerValidator = familiemedlemmerValidator,
        vedleggValgValidator = vedleggValgValidator,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(
            arbeidsgiverensVirksomhetValidator,
            utenlandsoppdragetValidator,
            utsendingsperiodeOgLandValidator,
            arbeidstakerensLonnValidator,
            arbeidsstedIUtlandetValidator,
            tilleggsopplysningerValidator,
            arbeidssituasjonValidator,
            skatteforholdOgInntektValidator,
            familiemedlemmerValidator,
            vedleggValgValidator,
        )
        every { arbeidsgiverensVirksomhetValidator.validate(any(), any(), any(), any()) } returns emptyList()
        every { utenlandsoppdragetValidator.validate(any(), any()) } returns emptyList()
        every { utsendingsperiodeOgLandValidator.validate(any()) } returns emptyList()
        every { arbeidstakerensLonnValidator.validate(any()) } returns emptyList()
        every { arbeidsstedIUtlandetValidator.validate(any(), any(), any()) } returns emptyList()
        every { tilleggsopplysningerValidator.validate(any()) } returns emptyList()
        every { arbeidssituasjonValidator.validate(any(), any()) } returns emptyList()
        every { skatteforholdOgInntektValidator.validate(any(), any(), any()) } returns emptyList()
        every { familiemedlemmerValidator.validate(any()) } returns emptyList()
        every { vedleggValgValidator.validate(any()) } returns emptyList()
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("skjemaDataMedForventedeValidatorer")
    fun `skal kalle riktige felt-validatorer for hver implementasjon`(
        skjemaData: UtsendtArbeidstakerSkjemaData,
        beskrivelse: String,
        forventedeKall: List<() -> Unit>,
        uforventedeKall: List<() -> Unit>
    ) {
        validator.validateUtsendtArbeidstakerSkjemaData(
            skjemaData,
            flyt = privatFlyt,
            erOffentligArbeidsgiver = false,
            antallAnsatte = 50,
            erNorskregistrertUtenlandskForetak = false
        )

        forventedeKall.forEach { verifisering -> verifisering() }
        uforventedeKall.forEach { verifisering -> verifisering() }
    }

    fun skjemaDataMedForventedeValidatorer(): Stream<Arguments> = Stream.of(
        Arguments.of(
            UtsendtArbeidstakerArbeidsgiversSkjemaDataDto(),
            "ArbeidsgiversSkjemaData",
            listOf(
                { verify { utsendingsperiodeOgLandValidator.validate(any()) } },
                { verify { tilleggsopplysningerValidator.validate(any()) } },
                { verify { vedleggValgValidator.validate(any()) } },
                { verify { arbeidsgiverensVirksomhetValidator.validate(any(), any(), any(), any()) } },
                { verify { utenlandsoppdragetValidator.validate(any(), any()) } },
                { verify { arbeidstakerensLonnValidator.validate(any()) } },
                { verify { arbeidsstedIUtlandetValidator.validate(any(), any(), any()) } },
            ),
            listOf(
                { verify(exactly = 0) { arbeidssituasjonValidator.validate(any(), any()) } },
                { verify(exactly = 0) { skatteforholdOgInntektValidator.validate(any(), any(), any()) } },
                { verify(exactly = 0) { familiemedlemmerValidator.validate(any()) } },
            )
        ),
        Arguments.of(
            UtsendtArbeidstakerArbeidstakersSkjemaDataDto(),
            "ArbeidstakersSkjemaData",
            listOf(
                { verify { utsendingsperiodeOgLandValidator.validate(any()) } },
                { verify { tilleggsopplysningerValidator.validate(any()) } },
                { verify { vedleggValgValidator.validate(any()) } },
                { verify { arbeidssituasjonValidator.validate(any(), any()) } },
                { verify { skatteforholdOgInntektValidator.validate(any(), any(), any()) } },
                { verify { familiemedlemmerValidator.validate(any()) } },
                { verify { arbeidsstedIUtlandetValidator.validate(any(), any(), any()) } },
            ),
            listOf(
                { verify(exactly = 0) { arbeidsgiverensVirksomhetValidator.validate(any(), any(), any(), any()) } },
                { verify(exactly = 0) { utenlandsoppdragetValidator.validate(any(), any()) } },
                { verify(exactly = 0) { arbeidstakerensLonnValidator.validate(any()) } },
            )
        ),
        Arguments.of(
            UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto(),
            "ArbeidsgiverOgArbeidstakerSkjemaData",
            listOf(
                { verify { utsendingsperiodeOgLandValidator.validate(any()) } },
                { verify { tilleggsopplysningerValidator.validate(any()) } },
                { verify { vedleggValgValidator.validate(any()) } },
                { verify { arbeidsgiverensVirksomhetValidator.validate(any(), any(), any(), any()) } },
                { verify { utenlandsoppdragetValidator.validate(any(), any()) } },
                { verify { arbeidstakerensLonnValidator.validate(any()) } },
                { verify { arbeidsstedIUtlandetValidator.validate(any(), any(), any()) } },
                { verify { arbeidssituasjonValidator.validate(any(), any()) } },
                { verify { skatteforholdOgInntektValidator.validate(any(), any(), any()) } },
                { verify { familiemedlemmerValidator.validate(any()) } },
            ),
            emptyList<() -> Unit>()
        ),
    )

    @Test
    fun `skal sende flyten videre til felt-validatorene`() {
        val flyt = UtsendtArbeidstakerFlyt(
            utenlandsoppdragetSkalOppgis = false,
            arbeidsstedSkalOppgis = true,
            forenkletArbeidssted = true,
            forenkletArbeidstakerdel = true,
            lonnOppgisISkatteforhold = true,
        )

        validator.validateUtsendtArbeidstakerSkjemaData(
            UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto(),
            flyt = flyt,
            erOffentligArbeidsgiver = true,
            antallAnsatte = 50,
            erNorskregistrertUtenlandskForetak = false
        )

        verify { utenlandsoppdragetValidator.validate(any(), false) }
        verify { arbeidsstedIUtlandetValidator.validate(any(), true, true) }
        verify { arbeidssituasjonValidator.validate(any(), true) }
        verify { skatteforholdOgInntektValidator.validate(any(), true, true) }
    }

    @Test
    fun `skal kaste ValidationException når validering feiler`() {
        every { utsendingsperiodeOgLandValidator.validate(any()) } returns listOf(
            Violation(field = "utsendingsperiodeOgLand", translationKey = FELT_ER_PAAKREVD)
        )

        shouldThrow<ValidationException> {
            validator.validateUtsendtArbeidstakerSkjemaData(
                UtsendtArbeidstakerArbeidstakersSkjemaDataDto(),
                flyt = privatFlyt,
                erOffentligArbeidsgiver = false,
                antallAnsatte = 50,
                erNorskregistrertUtenlandskForetak = false
            )
        }
    }
}
