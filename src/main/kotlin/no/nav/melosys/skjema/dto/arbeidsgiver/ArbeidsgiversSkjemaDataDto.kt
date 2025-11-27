package no.nav.melosys.skjema.dto.arbeidsgiver

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidstakeren.ArbeidstakerenDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.dto.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.dto.felles.TilleggsopplysningerDto

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidsgiversSkjemaDataDto(
    val arbeidstakeren: ArbeidstakerenDto? = null,
    val arbeidsgiverensVirksomhetINorge: ArbeidsgiverensVirksomhetINorgeDto? = null,
    val utenlandsoppdraget: UtenlandsoppdragetDto? = null,
    val arbeidstakerensLonn: ArbeidstakerensLonnDto? = null,
    val arbeidsstedIUtlandet: ArbeidsstedIUtlandetDto? = null,
    val tilleggsopplysninger: TilleggsopplysningerDto? = null
)