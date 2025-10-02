package no.nav.melosys.skjema.controller

import java.time.LocalDate

data class UtenlandsoppdragRequest(
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