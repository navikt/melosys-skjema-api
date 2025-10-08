package no.nav.melosys.skjema.dto

data class ArbeidsgiversSkjemaDto(
    val arbeidsgiveren: ArbeidsgiverenDto? = null,
    val arbeidsgiverensVirksomhetINorge: ArbeidsgiverensVirksomhetINorgeDto? = null,
    val utenlandsoppdraget: UtenlandsoppdragetDto? = null,
    val arbeidstakerensLonn: ArbeidstakerensLonnDto? = null
)