package no.nav.melosys.skjema.dto

import jakarta.validation.constraints.NotBlank

data class OpprettSoknadMedKontekstRequest(
    val representasjonstype: Representasjonstype,
    val radgiverfirma: SimpleOrganisasjonDto?,
    val arbeidsgiver: SimpleOrganisasjonDto,
    val arbeidstaker: PersonDto,
    val harFullmakt: Boolean
)

data class PersonDto(
    @field:NotBlank
    val fnr: String,
    val etternavn: String? = null  // Kun nødvendig for PDL-validering uten fullmakt
)

data class SimpleOrganisasjonDto(
    @field:NotBlank
    val orgnr: String,
    @field:NotBlank
    val navn: String
)