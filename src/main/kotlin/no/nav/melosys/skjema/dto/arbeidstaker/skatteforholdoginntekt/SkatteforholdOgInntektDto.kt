package no.nav.melosys.skjema.dto.arbeidstaker.skatteforholdoginntekt

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.skjema.controller.validators.skatteforholdoginntekt.GyldigSkatteforholdOgInntekt

@JsonInclude(JsonInclude.Include.NON_NULL)
@GyldigSkatteforholdOgInntekt
data class SkatteforholdOgInntektDto(
    val erSkattepliktigTilNorgeIHeleutsendingsperioden: Boolean,
    val mottarPengestotteFraAnnetEosLandEllerSveits: Boolean,
    val landSomUtbetalerPengestotte: String?,
    val pengestotteSomMottasFraAndreLandBelop: String?,
    val pengestotteSomMottasFraAndreLandBeskrivelse: String?
)