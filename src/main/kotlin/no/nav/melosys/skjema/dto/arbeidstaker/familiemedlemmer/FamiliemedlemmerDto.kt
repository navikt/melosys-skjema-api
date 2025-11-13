package no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FamiliemedlemmerDto(
    val sokerForBarnUnder18SomSkalVaereMed: Boolean,
    val harEktefellePartnerSamboerEllerBarnOver18SomSenderEgenSoknad: Boolean
)