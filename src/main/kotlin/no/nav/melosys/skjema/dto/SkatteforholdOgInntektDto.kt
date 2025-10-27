package no.nav.melosys.skjema.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SkatteforholdOgInntektDto(
    val erSkattepliktigTilNorgeIHeleutsendingsperioden: Boolean,
    val mottarPengestotteFraAnnetEosLandEllerSveits: Boolean,
    val landSomUtbetalerPengestotte: String?,
    val pengestotteSomMottasFraAndreLandBelop: String?,
    val pengestotteSomMottasFraAndreLandBeskrivelse: String?
)