package no.nav.melosys.skjema.types.arbeidsgiver

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ArbeidsgiversSkjemaDataDto(
    val skjemaId: UUID? = null,
    val innsendtDato: Instant? = null,
    val erstatterSkjemaId: UUID? = null,
    val arbeidsgiverensVirksomhetINorge: ArbeidsgiverensVirksomhetINorgeDto? = null,
    val utenlandsoppdraget: UtenlandsoppdragetDto? = null,
    val arbeidstakerensLonn: ArbeidstakerensLonnDto? = null,
    val arbeidsstedIUtlandet: ArbeidsstedIUtlandetDto? = null,
    val tilleggsopplysninger: TilleggsopplysningerDto? = null
)