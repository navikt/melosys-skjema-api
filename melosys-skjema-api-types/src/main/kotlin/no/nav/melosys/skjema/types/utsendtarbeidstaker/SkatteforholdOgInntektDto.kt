package no.nav.melosys.skjema.types.utsendtarbeidstaker

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SkatteforholdOgInntektDto(
    val erSkattepliktigTilNorgeIHeleutsendingsperioden: Boolean,
    val mottarPengestotteFraAnnetEosLandEllerSveits: Boolean,
    val landSomUtbetalerPengestotte: String?,
    val pengestotteSomMottasFraAndreLandBelop: String?,
    val pengestotteSomMottasFraAndreLandBeskrivelse: String?,
    val arbeidsinntektFraNorskEllerUtenlandskVirksomhet: Map<String, Boolean>? = null,
    val hvilkenInntektHarDu: Map<String, Boolean>? = null,
    val inntekterFraNorskVirksomhet: String? = null,
    val inntekterFraEgenVirksomhet: String? = null
)