package no.nav.melosys.skjema.types.utsendtarbeidstaker

import com.fasterxml.jackson.annotation.JsonInclude

enum class ArbeidsinntektKilde {
    NORSK_VIRKSOMHET,
    UTENLANDSK_VIRKSOMHET,
}

enum class InntektType {
    LOENN,
    INNTEKT_FRA_EGEN_VIRKSOMHET,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SkatteforholdOgInntektDto(
    val erSkattepliktigTilNorgeIHeleutsendingsperioden: Boolean,
    val mottarPengestotteFraAnnetEosLandEllerSveits: Boolean,
    val landSomUtbetalerPengestotte: String?,
    val pengestotteSomMottasFraAndreLandBelop: String?,
    val pengestotteSomMottasFraAndreLandBeskrivelse: String?,
    val inntektFraNorskEllerUtenlandskVirksomhet: Map<String, Boolean>? = null,
    val hvilkeTyperInntektHarDu: Map<String, Boolean>? = null,
    val inntekt: String? = null,
    val inntektFraEgenVirksomhet: String? = null
)