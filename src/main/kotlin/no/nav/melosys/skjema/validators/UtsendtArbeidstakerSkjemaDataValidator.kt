package no.nav.melosys.skjema.validators

import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtenlandsoppdragetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FamiliemedlemmerDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.types.felles.PeriodeDto
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.types.felles.VedleggValgDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.validators.arbeidsgiverensvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeValidator
import no.nav.melosys.skjema.validators.arbeidssituasjon.ArbeidssituasjonValidator
import no.nav.melosys.skjema.validators.arbeidsstediutlandet.ArbeidsstedIUtlandetValidator
import no.nav.melosys.skjema.validators.arbeidstakerenslonn.ArbeidstakerensLonnValidator
import no.nav.melosys.skjema.validators.familiemedlemmer.FamiliemedlemmerValidator
import no.nav.melosys.skjema.validators.felles.PeriodeValidator
import no.nav.melosys.skjema.validators.skatteforholdoginntekt.SkatteforholdOgInntektValidator
import no.nav.melosys.skjema.validators.tilleggsopplysninger.TilleggsopplysningerValidator
import no.nav.melosys.skjema.validators.utenlandsoppdraget.UtenlandsoppdragetValidator
import no.nav.melosys.skjema.validators.utsendingsperiodeogland.UtsendingsperiodeOgLandValidator
import no.nav.melosys.skjema.validators.vedlegg.VedleggValgValidator
import org.springframework.stereotype.Component

@Component
class UtsendtArbeidstakerSkjemaDataValidator(
    private val arbeidsgiverensVirksomhetValidator: ArbeidsgiverensVirksomhetINorgeValidator,
    private val utenlandsoppdragetValidator: UtenlandsoppdragetValidator,
    private val utsendingsperiodeOgLandValidator: UtsendingsperiodeOgLandValidator,
    private val arbeidstakerensLonnValidator: ArbeidstakerensLonnValidator,
    private val arbeidsstedIUtlandetValidator: ArbeidsstedIUtlandetValidator,
    private val tilleggsopplysningerValidator: TilleggsopplysningerValidator,
    private val arbeidssituasjonValidator: ArbeidssituasjonValidator,
    private val skatteforholdOgInntektValidator: SkatteforholdOgInntektValidator,
    private val familiemedlemmerValidator: FamiliemedlemmerValidator,
    private val vedleggValgValidator: VedleggValgValidator,
) {
    fun validate(
        dto: ArbeidsgiverensVirksomhetINorgeDto?,
        erOffentligArbeidsgiver: Boolean?,
        antallAnsatte: Int?,
        erNorskregistrertUtenlandskForetak: Boolean?
    ) {
        throwIfViolations(
            arbeidsgiverensVirksomhetValidator.validate(dto, erOffentligArbeidsgiver, antallAnsatte, erNorskregistrertUtenlandskForetak)
        )
    }

    fun validate(dto: UtenlandsoppdragetDto?, flyt: UtsendtArbeidstakerFlyt) {
        throwIfViolations(utenlandsoppdragetValidator.validate(dto, flyt.utenlandsoppdragetSkalOppgis))
    }

    fun validate(dto: UtsendingsperiodeOgLandDto) {
        throwIfViolations(utsendingsperiodeOgLandValidator.validate(dto))
    }

    fun validate(dto: PeriodeDto) {
        throwIfViolations(PeriodeValidator.validate(dto))
    }

    fun validate(dto: ArbeidstakerensLonnDto?) {
        throwIfViolations(arbeidstakerensLonnValidator.validate(dto))
    }

    fun validate(dto: ArbeidsstedIUtlandetDto?, flyt: UtsendtArbeidstakerFlyt) {
        throwIfViolations(validateArbeidssted(dto, flyt))
    }

    fun validate(dto: TilleggsopplysningerDto?) {
        throwIfViolations(tilleggsopplysningerValidator.validate(dto))
    }

    fun validate(dto: ArbeidssituasjonDto?, flyt: UtsendtArbeidstakerFlyt) {
        throwIfViolations(arbeidssituasjonValidator.validate(dto, flyt.forenkletArbeidstakerdel))
    }

    fun validate(dto: SkatteforholdOgInntektDto?, flyt: UtsendtArbeidstakerFlyt) {
        throwIfViolations(validateSkatteforhold(dto, flyt))
    }

    fun validate(dto: FamiliemedlemmerDto?) {
        throwIfViolations(familiemedlemmerValidator.validate(dto))
    }

    fun validate(dto: VedleggValgDto?) {
        throwIfViolations(vedleggValgValidator.validate(dto))
    }

    fun validateUtsendtArbeidstakerSkjemaData(
        skjemaData: UtsendtArbeidstakerSkjemaData,
        flyt: UtsendtArbeidstakerFlyt,
        erOffentligArbeidsgiver: Boolean?,
        antallAnsatte: Int?,
        erNorskregistrertUtenlandskForetak: Boolean?
    ) {
        val violations = mutableListOf<Violation>()

        violations += utsendingsperiodeOgLandValidator.validate(skjemaData.utsendingsperiodeOgLand)
        violations += tilleggsopplysningerValidator.validate(skjemaData.tilleggsopplysninger)
        violations += vedleggValgValidator.validate(skjemaData.vedlegg)

        when (skjemaData) {
            is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> {
                violations += arbeidsgiverensVirksomhetValidator.validate(
                    skjemaData.arbeidsgiverensVirksomhetINorge,
                    erOffentligArbeidsgiver,
                    antallAnsatte,
                    erNorskregistrertUtenlandskForetak
                )
                violations += utenlandsoppdragetValidator.validate(skjemaData.utenlandsoppdraget, flyt.utenlandsoppdragetSkalOppgis)
                violations += arbeidstakerensLonnValidator.validate(skjemaData.arbeidstakerensLonn)
                violations += validateArbeidssted(skjemaData.arbeidsstedIUtlandet, flyt)
            }
            is UtsendtArbeidstakerArbeidstakersSkjemaDataDto -> {
                violations += validateArbeidssted(skjemaData.arbeidsstedIUtlandet, flyt)
                violations += arbeidssituasjonValidator.validate(skjemaData.arbeidssituasjon, flyt.forenkletArbeidstakerdel)
                violations += validateSkatteforhold(skjemaData.skatteforholdOgInntekt, flyt)
                violations += familiemedlemmerValidator.validate(skjemaData.familiemedlemmer)
            }
            is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> {
                violations += arbeidsgiverensVirksomhetValidator.validate(
                    skjemaData.arbeidsgiversData.arbeidsgiverensVirksomhetINorge,
                    erOffentligArbeidsgiver,
                    antallAnsatte,
                    erNorskregistrertUtenlandskForetak
                )
                violations += utenlandsoppdragetValidator.validate(skjemaData.arbeidsgiversData.utenlandsoppdraget, flyt.utenlandsoppdragetSkalOppgis)
                violations += arbeidstakerensLonnValidator.validate(skjemaData.arbeidsgiversData.arbeidstakerensLonn)
                violations += validateArbeidssted(skjemaData.arbeidsgiversData.arbeidsstedIUtlandet, flyt)
                violations += arbeidssituasjonValidator.validate(skjemaData.arbeidstakersData.arbeidssituasjon, flyt.forenkletArbeidstakerdel)
                violations += validateSkatteforhold(skjemaData.arbeidstakersData.skatteforholdOgInntekt, flyt)
                violations += familiemedlemmerValidator.validate(skjemaData.arbeidstakersData.familiemedlemmer)
            }
        }

        throwIfViolations(violations)
    }

    private fun validateArbeidssted(dto: ArbeidsstedIUtlandetDto?, flyt: UtsendtArbeidstakerFlyt) =
        arbeidsstedIUtlandetValidator.validate(dto, flyt.arbeidsstedSkalOppgis, flyt.forenkletArbeidssted)

    private fun validateSkatteforhold(dto: SkatteforholdOgInntektDto?, flyt: UtsendtArbeidstakerFlyt) =
        skatteforholdOgInntektValidator.validate(dto, flyt.forenkletArbeidstakerdel, flyt.lonnOppgisISkatteforhold)

    private fun throwIfViolations(violations: List<Violation>) {
        if (violations.isNotEmpty()) {
            throw ValidationException(violations)
        }
    }
}
