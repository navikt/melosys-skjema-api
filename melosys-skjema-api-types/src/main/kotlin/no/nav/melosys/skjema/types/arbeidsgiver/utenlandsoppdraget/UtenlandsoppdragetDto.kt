package no.nav.melosys.skjema.types.arbeidsgiver.utenlandsoppdraget

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.types.felles.PeriodeDto

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtenlandsoppdragetDto(
    val arbeidsgiverHarOppdragILandet: Boolean,
    val arbeidstakerBleAnsattForUtenlandsoppdraget: Boolean,
    val arbeidstakerForblirAnsattIHelePerioden: Boolean,
    val arbeidstakerErstatterAnnenPerson: Boolean,
    val arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget: Boolean?,
    val utenlandsoppholdetsBegrunnelse: String?,
    val ansettelsesforholdBeskrivelse: String?,
    val forrigeArbeidstakerUtsendelsePeriode: PeriodeDto?
)