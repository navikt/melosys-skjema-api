package no.nav.melosys.skjema.dto.arbeidsgiver.utenlandsoppdraget

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtenlandsoppdragetDto(
    val utsendelseLand: String,
    val arbeidstakerUtsendelseFraDato: LocalDate,
    val arbeidstakerUtsendelseTilDato: LocalDate,
    val arbeidsgiverHarOppdragILandet: Boolean,
    val arbeidstakerBleAnsattForUtenlandsoppdraget: Boolean,
    val arbeidstakerForblirAnsattIHelePerioden: Boolean,
    val arbeidstakerErstatterAnnenPerson: Boolean,
    val arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget: Boolean?,
    val utenlandsoppholdetsBegrunnelse: String?,
    val ansettelsesforholdBeskrivelse: String?,
    val forrigeArbeidstakerUtsendelseFradato: LocalDate?,
    val forrigeArbeidstakerUtsendelseTilDato: LocalDate?
)