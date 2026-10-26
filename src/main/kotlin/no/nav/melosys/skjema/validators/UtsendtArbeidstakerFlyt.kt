package no.nav.melosys.skjema.validators

import java.util.UUID
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData

/**
 * Hvilke spørsmål skjemadelen skal ha svar på, gitt arbeidsgivertype og AAREG-treff.
 *
 * - Statlig arbeidsgiver (arbeidsgivers del og kombinert skjema): utenlandsoppdraget faller bort,
 *   og arbeidssted oppgis som «på land» uten spørsmål om hjemmekontor.
 * - Statsansatt (statlig arbeidsgiver + arbeidsforhold i AAREG, arbeidstakers del og
 *   kombinert skjema): arbeidssituasjon og skatteforhold reduseres.
 * - Statsansatt i arbeidstakers del som sender før arbeidsgiver (ikke forhåndsutfylt fra en
 *   innsendt arbeidsgivers del): arbeidstaker oppgir selv arbeidssted på land og svarer på lønnsspørsmålet.
 */
data class UtsendtArbeidstakerFlyt(
    val utenlandsoppdragetSkalOppgis: Boolean,
    val arbeidsstedSkalOppgis: Boolean,
    val arbeidsstedForStatsansatt: Boolean,
    val arbeidstakerdelForStatsansatt: Boolean,
    val lonnOppgisISkatteforhold: Boolean,
) {
    /**
     * Fjerner lagrede svar i arbeidstakerens spørsmål som ikke hører til flyten. Brukes når AAREG-treffet
     * kan ha snudd fordi perioden er endret. Felt som blir påkrevd, fanges av valideringen ved innsending.
     */
    fun fjernSvarUtenforFlyten(data: UtsendtArbeidstakerSkjemaData): UtsendtArbeidstakerSkjemaData = when (data) {
        is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> data
        is UtsendtArbeidstakerArbeidstakersSkjemaDataDto -> data.copy(
            arbeidsstedIUtlandet = data.arbeidsstedIUtlandet.takeIf { arbeidsstedSkalOppgis },
            arbeidssituasjon = data.arbeidssituasjon?.let(::tilpassArbeidssituasjon),
            skatteforholdOgInntekt = data.skatteforholdOgInntekt?.let(::tilpassSkatteforhold),
        )
        is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> data.copy(
            arbeidstakersData = data.arbeidstakersData.copy(
                arbeidssituasjon = data.arbeidstakersData.arbeidssituasjon?.let(::tilpassArbeidssituasjon),
                skatteforholdOgInntekt = data.arbeidstakersData.skatteforholdOgInntekt?.let(::tilpassSkatteforhold),
            )
        )
    }

    private fun tilpassArbeidssituasjon(dto: ArbeidssituasjonDto): ArbeidssituasjonDto =
        if (arbeidstakerdelForStatsansatt) {
            dto.copy(harVaertEllerSkalVaereILonnetArbeidFoerUtsending = null, aktivitetIMaanedenFoerUtsendingen = null)
        } else dto

    private fun tilpassSkatteforhold(dto: SkatteforholdOgInntektDto): SkatteforholdOgInntektDto {
        val utenInntekt = if (arbeidstakerdelForStatsansatt) {
            dto.copy(
                inntektFraNorskEllerUtenlandskVirksomhet = null,
                hvilkeTyperInntektHarDu = null,
                inntekt = null,
                inntektFraEgenVirksomhet = null,
            )
        } else dto
        return if (lonnOppgisISkatteforhold) utenInntekt else utenInntekt.copy(
            arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden = null,
            virksomheterSomUtbetalerLonnOgNaturalytelser = null,
        )
    }

    companion object {
        fun fra(
            metadata: UtsendtArbeidstakerMetadata,
            prefyltFraSkjemaId: UUID?,
            statsansattFlytAktivert: Boolean,
        ): UtsendtArbeidstakerFlyt {
            // MIDLERTIDIG MELOSYS-7694: fjern gammel-flyt-grenen når statsansattflyten er permanent.
            if (!statsansattFlytAktivert) {
                val harArbeidsgiverdel = metadata.skjemadel != Skjemadel.ARBEIDSTAKERS_DEL
                return UtsendtArbeidstakerFlyt(
                    utenlandsoppdragetSkalOppgis = harArbeidsgiverdel,
                    arbeidsstedSkalOppgis = harArbeidsgiverdel,
                    arbeidsstedForStatsansatt = false,
                    arbeidstakerdelForStatsansatt = false,
                    lonnOppgisISkatteforhold = false,
                )
            }
            val erStatligArbeidsgiver = metadata.erStatligArbeidsgiver == true
            val erStatsansatt = erStatligArbeidsgiver && metadata.harArbeidsforholdIAareg == true

            return when (metadata.skjemadel) {
                Skjemadel.ARBEIDSGIVERS_DEL, Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL -> UtsendtArbeidstakerFlyt(
                    utenlandsoppdragetSkalOppgis = !erStatligArbeidsgiver,
                    arbeidsstedSkalOppgis = true,
                    arbeidsstedForStatsansatt = erStatligArbeidsgiver,
                    arbeidstakerdelForStatsansatt = metadata.skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL &&
                        erStatsansatt,
                    lonnOppgisISkatteforhold = false,
                )
                Skjemadel.ARBEIDSTAKERS_DEL -> {
                    val arbeidstakerSenderFoerst = erStatsansatt && prefyltFraSkjemaId == null
                    UtsendtArbeidstakerFlyt(
                        utenlandsoppdragetSkalOppgis = false,
                        arbeidsstedSkalOppgis = arbeidstakerSenderFoerst,
                        arbeidsstedForStatsansatt = true,
                        arbeidstakerdelForStatsansatt = erStatsansatt,
                        lonnOppgisISkatteforhold = arbeidstakerSenderFoerst,
                    )
                }
            }
        }
    }
}
