package no.nav.melosys.skjema.validators

import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtenlandsoppdragetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FamiliemedlemmerDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.SkatteforholdOgInntektDto
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
    fun validate(dto: ArbeidsgiverensVirksomhetINorgeDto?) {
        throwIfViolations(arbeidsgiverensVirksomhetValidator.validate(dto))
    }

    fun validate(dto: UtenlandsoppdragetDto?) {
        throwIfViolations(utenlandsoppdragetValidator.validate(dto))
    }

    fun validate(dto: UtsendingsperiodeOgLandDto) {
        throwIfViolations(utsendingsperiodeOgLandValidator.validate(dto))
    }

    fun validate(dto: ArbeidstakerensLonnDto?) {
        throwIfViolations(arbeidstakerensLonnValidator.validate(dto))
    }

    fun validate(dto: ArbeidsstedIUtlandetDto?) {
        throwIfViolations(arbeidsstedIUtlandetValidator.validate(dto))
    }

    fun validate(dto: TilleggsopplysningerDto?) {
        throwIfViolations(tilleggsopplysningerValidator.validate(dto))
    }

    fun validate(dto: ArbeidssituasjonDto?) {
        throwIfViolations(arbeidssituasjonValidator.validate(dto))
    }

    fun validate(dto: SkatteforholdOgInntektDto?) {
        throwIfViolations(skatteforholdOgInntektValidator.validate(dto))
    }

    fun validate(dto: FamiliemedlemmerDto?) {
        throwIfViolations(familiemedlemmerValidator.validate(dto))
    }

    fun validate(dto: VedleggValgDto?) {
        throwIfViolations(vedleggValgValidator.validate(dto))
    }

    fun validateUtsendtArbeidstakerSkjemaData(skjemaData: UtsendtArbeidstakerSkjemaData) {
        val violations = mutableListOf<Violation>()

        violations += utsendingsperiodeOgLandValidator.validate(skjemaData.utsendingsperiodeOgLand)
        violations += tilleggsopplysningerValidator.validate(skjemaData.tilleggsopplysninger)
        violations += vedleggValgValidator.validate(skjemaData.vedlegg)

        when (skjemaData) {
            is UtsendtArbeidstakerArbeidsgiversSkjemaDataDto -> {
                violations += arbeidsgiverensVirksomhetValidator.validate(skjemaData.arbeidsgiverensVirksomhetINorge)
                violations += utenlandsoppdragetValidator.validate(skjemaData.utenlandsoppdraget)
                violations += arbeidstakerensLonnValidator.validate(skjemaData.arbeidstakerensLonn)
                violations += arbeidsstedIUtlandetValidator.validate(skjemaData.arbeidsstedIUtlandet)
            }
            is UtsendtArbeidstakerArbeidstakersSkjemaDataDto -> {
                violations += arbeidssituasjonValidator.validate(skjemaData.arbeidssituasjon)
                violations += skatteforholdOgInntektValidator.validate(skjemaData.skatteforholdOgInntekt)
                violations += familiemedlemmerValidator.validate(skjemaData.familiemedlemmer)
            }
            is UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto -> {
                violations += arbeidsgiverensVirksomhetValidator.validate(skjemaData.arbeidsgiversData.arbeidsgiverensVirksomhetINorge)
                violations += utenlandsoppdragetValidator.validate(skjemaData.arbeidsgiversData.utenlandsoppdraget)
                violations += arbeidstakerensLonnValidator.validate(skjemaData.arbeidsgiversData.arbeidstakerensLonn)
                violations += arbeidsstedIUtlandetValidator.validate(skjemaData.arbeidsgiversData.arbeidsstedIUtlandet)
                violations += arbeidssituasjonValidator.validate(skjemaData.arbeidstakersData.arbeidssituasjon)
                violations += skatteforholdOgInntektValidator.validate(skjemaData.arbeidstakersData.skatteforholdOgInntekt)
                violations += familiemedlemmerValidator.validate(skjemaData.arbeidstakersData.familiemedlemmer)
            }
        }

        throwIfViolations(violations)
    }

    private fun throwIfViolations(violations: List<Violation>) {
        if (violations.isNotEmpty()) {
            throw ValidationException(violations)
        }
    }
}
