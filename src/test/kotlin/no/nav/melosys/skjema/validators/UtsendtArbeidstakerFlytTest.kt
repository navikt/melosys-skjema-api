package no.nav.melosys.skjema.validators

import io.kotest.matchers.shouldBe
import java.util.UUID
import no.nav.melosys.skjema.arbeidsgiverOgArbeidstakerSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidsgiversSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidsstedIUtlandetDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakersSkjemaDataDtoMedDefaultVerdier
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.utsendtArbeidstakerMetadataMedDefaultVerdier
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class UtsendtArbeidstakerFlytTest {

    private val vanligFlyt = UtsendtArbeidstakerFlyt(
        utenlandsoppdragetSkalOppgis = true,
        arbeidsstedSkalOppgis = true,
        forenkletArbeidssted = false,
        forenkletArbeidstakerdel = false,
        lonnOppgisISkatteforhold = false,
    )

    private fun flyt(
        skjemadel: Skjemadel,
        erOffentligArbeidsgiver: Boolean?,
        harArbeidsforholdIAareg: Boolean?,
        prefyltFraSkjemaId: UUID? = null,
    ) = UtsendtArbeidstakerFlyt.fra(
        utsendtArbeidstakerMetadataMedDefaultVerdier(skjemadel = skjemadel, erOffentligArbeidsgiver = erOffentligArbeidsgiver)
            .medHarArbeidsforholdIAareg(harArbeidsforholdIAareg),
        prefyltFraSkjemaId,
    )

    @ParameterizedTest
    @EnumSource(Skjemadel::class, names = ["ARBEIDSGIVERS_DEL", "ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL"])
    fun `privat arbeidsgiver får vanlig flyt i arbeidsgivers del og kombinert skjema`(skjemadel: Skjemadel) {
        flyt(skjemadel, erOffentligArbeidsgiver = false, harArbeidsforholdIAareg = true) shouldBe vanligFlyt
        flyt(skjemadel, erOffentligArbeidsgiver = null, harArbeidsforholdIAareg = true) shouldBe vanligFlyt
    }

    @Test
    fun `offentlig arbeidsgiver uten utenlandsoppdraget og med forenklet arbeidssted i arbeidsgivers del`() {
        val forventet = vanligFlyt.copy(utenlandsoppdragetSkalOppgis = false, forenkletArbeidssted = true)

        flyt(Skjemadel.ARBEIDSGIVERS_DEL, erOffentligArbeidsgiver = true, harArbeidsforholdIAareg = true) shouldBe forventet
        flyt(Skjemadel.ARBEIDSGIVERS_DEL, erOffentligArbeidsgiver = true, harArbeidsforholdIAareg = false) shouldBe forventet
    }

    @Test
    fun `kombinert skjema forenkler arbeidstakerdelen bare for offentlig tjenesteperson`() {
        val offentligAg = vanligFlyt.copy(utenlandsoppdragetSkalOppgis = false, forenkletArbeidssted = true)

        flyt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, erOffentligArbeidsgiver = true, harArbeidsforholdIAareg = false) shouldBe offentligAg
        flyt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, erOffentligArbeidsgiver = true, harArbeidsforholdIAareg = null) shouldBe offentligAg
        flyt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, erOffentligArbeidsgiver = true, harArbeidsforholdIAareg = true) shouldBe
            offentligAg.copy(forenkletArbeidstakerdel = true)
    }

    @Test
    fun `arbeidstakers del uten offentlig tjenesteperson har verken utenlandsoppdraget, arbeidssted eller lønn`() {
        val forventet = UtsendtArbeidstakerFlyt(
            utenlandsoppdragetSkalOppgis = false,
            arbeidsstedSkalOppgis = false,
            forenkletArbeidssted = true,
            forenkletArbeidstakerdel = false,
            lonnOppgisISkatteforhold = false,
        )

        flyt(Skjemadel.ARBEIDSTAKERS_DEL, erOffentligArbeidsgiver = false, harArbeidsforholdIAareg = true) shouldBe forventet
        flyt(Skjemadel.ARBEIDSTAKERS_DEL, erOffentligArbeidsgiver = true, harArbeidsforholdIAareg = false) shouldBe forventet
        flyt(Skjemadel.ARBEIDSTAKERS_DEL, erOffentligArbeidsgiver = true, harArbeidsforholdIAareg = null) shouldBe forventet
    }

    @Test
    fun `offentlig tjenesteperson som sender før arbeidsgiver oppgir arbeidssted og lønn`() {
        flyt(Skjemadel.ARBEIDSTAKERS_DEL, erOffentligArbeidsgiver = true, harArbeidsforholdIAareg = true) shouldBe
            UtsendtArbeidstakerFlyt(
                utenlandsoppdragetSkalOppgis = false,
                arbeidsstedSkalOppgis = true,
                forenkletArbeidssted = true,
                forenkletArbeidstakerdel = true,
                lonnOppgisISkatteforhold = true,
            )
    }

    @Test
    fun `offentlig tjenesteperson forhåndsutfylt fra arbeidsgivers del oppgir ikke arbeidssted og lønn`() {
        flyt(
            Skjemadel.ARBEIDSTAKERS_DEL,
            erOffentligArbeidsgiver = true,
            harArbeidsforholdIAareg = true,
            prefyltFraSkjemaId = UUID.randomUUID()
        ) shouldBe UtsendtArbeidstakerFlyt(
            utenlandsoppdragetSkalOppgis = false,
            arbeidsstedSkalOppgis = false,
            forenkletArbeidssted = true,
            forenkletArbeidstakerdel = true,
            lonnOppgisISkatteforhold = false,
        )
    }

    @Test
    fun `fjernSvarUtenforFlyten lar arbeidsgivers del være urørt`() {
        val data = arbeidsgiversSkjemaDataDtoMedDefaultVerdier()

        vanligFlyt.copy(forenkletArbeidstakerdel = true).fjernSvarUtenforFlyten(data) shouldBe data
    }

    @Test
    fun `fjernSvarUtenforFlyten fjerner arbeidssted, lønn og inntekt som ikke hører til flyten i arbeidstakers del`() {
        val default = arbeidstakersSkjemaDataDtoMedDefaultVerdier()
        val data = default.copy(
            arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier(),
            skatteforholdOgInntekt = default.skatteforholdOgInntekt!!.copy(
                arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = true
            ),
        )
        val flyt = flyt(Skjemadel.ARBEIDSTAKERS_DEL, erOffentligArbeidsgiver = true, harArbeidsforholdIAareg = true, UUID.randomUUID())

        val resultat = flyt.fjernSvarUtenforFlyten(data) as UtsendtArbeidstakerArbeidstakersSkjemaDataDto

        resultat.arbeidsstedIUtlandet shouldBe null
        resultat.arbeidssituasjon shouldBe default.arbeidssituasjon!!.copy(
            harVaertEllerSkalVaereILonnetArbeidFoerUtsending = null,
            aktivitetIMaanedenFoerUtsendingen = null,
        )
        resultat.skatteforholdOgInntekt shouldBe default.skatteforholdOgInntekt!!.copy(
            inntektFraNorskEllerUtenlandskVirksomhet = null,
            hvilkeTyperInntektHarDu = null,
        )
        resultat.familiemedlemmer shouldBe default.familiemedlemmer
    }

    @Test
    fun `fjernSvarUtenforFlyten beholder svarene når offentlig tjenesteperson sender før arbeidsgiver`() {
        val default = arbeidstakersSkjemaDataDtoMedDefaultVerdier()
        val data = default.copy(
            arbeidsstedIUtlandet = arbeidsstedIUtlandetDtoMedDefaultVerdier(),
            arbeidssituasjon = default.arbeidssituasjon!!.copy(
                harVaertEllerSkalVaereILonnetArbeidFoerUtsending = null,
                aktivitetIMaanedenFoerUtsendingen = null,
            ),
            skatteforholdOgInntekt = default.skatteforholdOgInntekt!!.copy(
                inntektFraNorskEllerUtenlandskVirksomhet = null,
                hvilkeTyperInntektHarDu = null,
                arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = true,
            ),
        )
        val flyt = flyt(Skjemadel.ARBEIDSTAKERS_DEL, erOffentligArbeidsgiver = true, harArbeidsforholdIAareg = true)

        flyt.fjernSvarUtenforFlyten(data) shouldBe data
    }

    @Test
    fun `fjernSvarUtenforFlyten forenkler arbeidstakerdelen i kombinert skjema for offentlig tjenesteperson`() {
        val data = arbeidsgiverOgArbeidstakerSkjemaDataDtoMedDefaultVerdier()
        val flyt = flyt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, erOffentligArbeidsgiver = true, harArbeidsforholdIAareg = true)

        val resultat = flyt.fjernSvarUtenforFlyten(data) as UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto

        resultat.arbeidsgiversData shouldBe data.arbeidsgiversData
        resultat.arbeidstakersData.arbeidssituasjon!!.harVaertEllerSkalVaereILonnetArbeidFoerUtsending shouldBe null
        resultat.arbeidstakersData.skatteforholdOgInntekt!!.hvilkeTyperInntektHarDu shouldBe null
        resultat.arbeidstakersData.familiemedlemmer shouldBe data.arbeidstakersData.familiemedlemmer
    }
}
