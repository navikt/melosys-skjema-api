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
        arbeidsstedForStatsansatt = false,
        arbeidstakerdelForStatsansatt = false,
        lonnOppgisISkatteforhold = false,
    )

    private fun flyt(
        skjemadel: Skjemadel,
        erStatligArbeidsgiver: Boolean?,
        harArbeidsforholdIAareg: Boolean?,
        prefyltFraSkjemaId: UUID? = null,
    ) = UtsendtArbeidstakerFlyt.fra(
        utsendtArbeidstakerMetadataMedDefaultVerdier(skjemadel = skjemadel, erStatligArbeidsgiver = erStatligArbeidsgiver)
            .medHarArbeidsforholdIAareg(harArbeidsforholdIAareg),
        prefyltFraSkjemaId,
    )

    @ParameterizedTest
    @EnumSource(Skjemadel::class, names = ["ARBEIDSGIVERS_DEL", "ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL"])
    fun `ikke-statlig arbeidsgiver får vanlig flyt i arbeidsgivers del og kombinert skjema`(skjemadel: Skjemadel) {
        flyt(skjemadel, erStatligArbeidsgiver = false, harArbeidsforholdIAareg = true) shouldBe vanligFlyt
        flyt(skjemadel, erStatligArbeidsgiver = null, harArbeidsforholdIAareg = true) shouldBe vanligFlyt
    }

    @Test
    fun `statlig arbeidsgiver uten utenlandsoppdraget og med arbeidssted for statsansatt i arbeidsgivers del`() {
        val forventet = vanligFlyt.copy(utenlandsoppdragetSkalOppgis = false, arbeidsstedForStatsansatt = true)

        flyt(Skjemadel.ARBEIDSGIVERS_DEL, erStatligArbeidsgiver = true, harArbeidsforholdIAareg = true) shouldBe forventet
        flyt(Skjemadel.ARBEIDSGIVERS_DEL, erStatligArbeidsgiver = true, harArbeidsforholdIAareg = false) shouldBe forventet
    }

    @Test
    fun `kombinert skjema bruker statsansattflyten bare ved AAREG-treff`() {
        val statligArbeidsgiver = vanligFlyt.copy(utenlandsoppdragetSkalOppgis = false, arbeidsstedForStatsansatt = true)

        flyt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, erStatligArbeidsgiver = true, harArbeidsforholdIAareg = false) shouldBe statligArbeidsgiver
        flyt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, erStatligArbeidsgiver = true, harArbeidsforholdIAareg = null) shouldBe statligArbeidsgiver
        flyt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, erStatligArbeidsgiver = true, harArbeidsforholdIAareg = true) shouldBe
            statligArbeidsgiver.copy(arbeidstakerdelForStatsansatt = true)
    }

    @Test
    fun `arbeidstakers del uten statsansattflyt har verken utenlandsoppdraget, arbeidssted eller lønn`() {
        val forventet = UtsendtArbeidstakerFlyt(
            utenlandsoppdragetSkalOppgis = false,
            arbeidsstedSkalOppgis = false,
            arbeidsstedForStatsansatt = true,
            arbeidstakerdelForStatsansatt = false,
            lonnOppgisISkatteforhold = false,
        )

        flyt(Skjemadel.ARBEIDSTAKERS_DEL, erStatligArbeidsgiver = false, harArbeidsforholdIAareg = true) shouldBe forventet
        flyt(Skjemadel.ARBEIDSTAKERS_DEL, erStatligArbeidsgiver = true, harArbeidsforholdIAareg = false) shouldBe forventet
        flyt(Skjemadel.ARBEIDSTAKERS_DEL, erStatligArbeidsgiver = true, harArbeidsforholdIAareg = null) shouldBe forventet
    }

    @Test
    fun `statsansatt som sender før arbeidsgiver oppgir arbeidssted og lønn`() {
        flyt(Skjemadel.ARBEIDSTAKERS_DEL, erStatligArbeidsgiver = true, harArbeidsforholdIAareg = true) shouldBe
            UtsendtArbeidstakerFlyt(
                utenlandsoppdragetSkalOppgis = false,
                arbeidsstedSkalOppgis = true,
                arbeidsstedForStatsansatt = true,
                arbeidstakerdelForStatsansatt = true,
                lonnOppgisISkatteforhold = true,
            )
    }

    @Test
    fun `statsansatt forhåndsutfylt fra arbeidsgivers del oppgir ikke arbeidssted og lønn`() {
        flyt(
            Skjemadel.ARBEIDSTAKERS_DEL,
            erStatligArbeidsgiver = true,
            harArbeidsforholdIAareg = true,
            prefyltFraSkjemaId = UUID.randomUUID()
        ) shouldBe UtsendtArbeidstakerFlyt(
            utenlandsoppdragetSkalOppgis = false,
            arbeidsstedSkalOppgis = false,
            arbeidsstedForStatsansatt = true,
            arbeidstakerdelForStatsansatt = true,
            lonnOppgisISkatteforhold = false,
        )
    }

    @Test
    fun `fjernSvarUtenforFlyten lar arbeidsgivers del være urørt`() {
        val data = arbeidsgiversSkjemaDataDtoMedDefaultVerdier()

        vanligFlyt.copy(arbeidstakerdelForStatsansatt = true).fjernSvarUtenforFlyten(data) shouldBe data
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
        val flyt = flyt(Skjemadel.ARBEIDSTAKERS_DEL, erStatligArbeidsgiver = true, harArbeidsforholdIAareg = true, UUID.randomUUID())

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
    fun `fjernSvarUtenforFlyten beholder svarene når statsansatt sender før arbeidsgiver`() {
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
        val flyt = flyt(Skjemadel.ARBEIDSTAKERS_DEL, erStatligArbeidsgiver = true, harArbeidsforholdIAareg = true)

        flyt.fjernSvarUtenforFlyten(data) shouldBe data
    }

    @Test
    fun `fjernSvarUtenforFlyten tilpasser arbeidstakerdelen til statsansattflyten i kombinert skjema`() {
        val data = arbeidsgiverOgArbeidstakerSkjemaDataDtoMedDefaultVerdier()
        val flyt = flyt(Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL, erStatligArbeidsgiver = true, harArbeidsforholdIAareg = true)

        val resultat = flyt.fjernSvarUtenforFlyten(data) as UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto

        resultat.arbeidsgiversData shouldBe data.arbeidsgiversData
        resultat.arbeidstakersData.arbeidssituasjon!!.harVaertEllerSkalVaereILonnetArbeidFoerUtsending shouldBe null
        resultat.arbeidstakersData.skatteforholdOgInntekt!!.hvilkeTyperInntektHarDu shouldBe null
        resultat.arbeidstakersData.familiemedlemmer shouldBe data.arbeidstakersData.familiemedlemmer
    }
}
