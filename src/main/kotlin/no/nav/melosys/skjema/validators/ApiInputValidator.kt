package no.nav.melosys.skjema.validators

import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.dto.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.dto.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.dto.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.dto.arbeidstaker.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelDto
import no.nav.melosys.skjema.dto.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.validators.arbeidsgiverensvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeValidator
import no.nav.melosys.skjema.validators.arbeidssituasjon.ArbeidssituasjonValidator
import no.nav.melosys.skjema.validators.arbeidsstediutlandet.ArbeidsstedIUtlandetValidator
import no.nav.melosys.skjema.validators.arbeidstakerenslonn.ArbeidstakerensLonnValidator
import no.nav.melosys.skjema.validators.familiemedlemmer.FamiliemedlemmerValidator
import no.nav.melosys.skjema.validators.skatteforholdoginntekt.SkatteforholdOgInntektValidator
import no.nav.melosys.skjema.validators.tilleggsopplysninger.TilleggsopplysningerValidator
import no.nav.melosys.skjema.validators.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelValidator
import no.nav.melosys.skjema.validators.utenlandsoppdraget.UtenlandsoppdragetValidator
import org.springframework.stereotype.Component

@Component
class ApiInputValidator(
    private val arbeidsgiverensVirksomhetValidator: ArbeidsgiverensVirksomhetINorgeValidator,
    private val utenlandsoppdragetValidator: UtenlandsoppdragetValidator,
    private val utenlandsoppdragetArbeidstakersDelValidator: UtenlandsoppdragetArbeidstakersDelValidator,
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

    fun validate(dto: UtenlandsoppdragetArbeidstakersDelDto) {
        throwIfViolations(utenlandsoppdragetArbeidstakersDelValidator.validate(dto))
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
