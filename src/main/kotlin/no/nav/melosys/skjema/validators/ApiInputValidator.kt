package no.nav.melosys.skjema.validators

import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtenlandsoppdragetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FamiliemedlemmerDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
import no.nav.melosys.skjema.validators.arbeidsgiverensvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeValidator
import no.nav.melosys.skjema.validators.arbeidssituasjon.ArbeidssituasjonValidator
import no.nav.melosys.skjema.validators.arbeidsstediutlandet.ArbeidsstedIUtlandetValidator
import no.nav.melosys.skjema.validators.arbeidstakerenslonn.ArbeidstakerensLonnValidator
import no.nav.melosys.skjema.validators.familiemedlemmer.FamiliemedlemmerValidator
import no.nav.melosys.skjema.validators.skatteforholdoginntekt.SkatteforholdOgInntektValidator
import no.nav.melosys.skjema.validators.tilleggsopplysninger.TilleggsopplysningerValidator
import no.nav.melosys.skjema.validators.utenlandsoppdraget.UtenlandsoppdragetValidator
import no.nav.melosys.skjema.validators.utsendingsperiodeogland.UtsendingsperiodeOgLandValidator
import org.springframework.stereotype.Component

@Component
class ApiInputValidator(
    private val arbeidsgiverensVirksomhetValidator: ArbeidsgiverensVirksomhetINorgeValidator,
    private val utenlandsoppdragetValidator: UtenlandsoppdragetValidator,
    private val utsendingsperiodeOgLandValidator: UtsendingsperiodeOgLandValidator,
    private val arbeidstakerensLonnValidator: ArbeidstakerensLonnValidator,
    private val arbeidsstedIUtlandetValidator: ArbeidsstedIUtlandetValidator,
    private val tilleggsopplysningerValidator: TilleggsopplysningerValidator,
    private val arbeidssituasjonValidator: ArbeidssituasjonValidator,
    private val skatteforholdOgInntektValidator: SkatteforholdOgInntektValidator,
    private val familiemedlemmerValidator: FamiliemedlemmerValidator,
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

    private fun throwIfViolations(violations: List<Violation>) {
        if (violations.isNotEmpty()) {
            throw ValidationException(violations)
        }
    }
}
