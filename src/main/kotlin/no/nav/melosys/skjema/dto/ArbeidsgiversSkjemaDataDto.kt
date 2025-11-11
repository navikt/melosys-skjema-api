package no.nav.melosys.skjema.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidsgiversSkjemaDataDto(
    val arbeidsgiveren: ArbeidsgiverenDto? = null,
    val arbeidstakeren: ArbeidstakerenArbeidsgiversDelDto? = null,
    val arbeidsgiverensVirksomhetINorge: ArbeidsgiverensVirksomhetINorgeDto? = null,
    val utenlandsoppdraget: UtenlandsoppdragetDto? = null,
    val arbeidstakerensLonn: ArbeidstakerensLonnDto? = null,
    val arbeidsstedIUtlandet: ArbeidsstedIUtlandetDto? = null
)