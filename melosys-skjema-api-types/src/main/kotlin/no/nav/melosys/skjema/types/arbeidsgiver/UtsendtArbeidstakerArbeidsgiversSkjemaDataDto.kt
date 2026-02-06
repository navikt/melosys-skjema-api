package no.nav.melosys.skjema.types.arbeidsgiver

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaData
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtsendtArbeidstakerArbeidsgiversSkjemaDataDto(
    override val type: String = "UTSENDT_ARBEIDSTAKER_ARBEIDSGIVERS_DEL",
    val arbeidsgiverensVirksomhetINorge: ArbeidsgiverensVirksomhetINorgeDto? = null,
    val utenlandsoppdraget: UtenlandsoppdragetDto? = null,
    val arbeidstakerensLonn: ArbeidstakerensLonnDto? = null,
    val arbeidsstedIUtlandet: ArbeidsstedIUtlandetDto? = null,
    val tilleggsopplysninger: TilleggsopplysningerDto? = null
) : UtsendtArbeidstakerSkjemaData