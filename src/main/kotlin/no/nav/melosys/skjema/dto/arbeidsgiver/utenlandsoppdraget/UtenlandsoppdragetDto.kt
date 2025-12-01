package no.nav.melosys.skjema.dto.arbeidsgiver.utenlandsoppdraget

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid
import no.nav.melosys.skjema.controller.validators.utenlandsoppdraget.GyldigUtenlandsoppdrag
import no.nav.melosys.skjema.dto.felles.PeriodeDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigUtenlandsoppdrag
data class UtenlandsoppdragetDto(
    val utsendelseLand: String,
    @field:Valid
    val arbeidstakerUtsendelsePeriode: PeriodeDto,
    val arbeidsgiverHarOppdragILandet: Boolean,
    val arbeidstakerBleAnsattForUtenlandsoppdraget: Boolean,
    val arbeidstakerForblirAnsattIHelePerioden: Boolean,
    val arbeidstakerErstatterAnnenPerson: Boolean,
    val arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget: Boolean?,
    val utenlandsoppholdetsBegrunnelse: String?,
    val ansettelsesforholdBeskrivelse: String?,
    @field:Valid
    val forrigeArbeidstakerUtsendelsePeriode: PeriodeDto?
)