package no.nav.melosys.skjema.translations.dto

data class SkatteforholdOgInntektTranslation(
    val maaOppgiLandSomUtbetalerPengestotte: String,
    val maaOppgiBelopPengestotte: String,
    val maaOppgiBeskrivelsePengestotte: String,
    val ugyldigBelopFormat: String,
    val maaVelgeMinstEnInntektKilde: String,
    val maaVelgeMinstEnInntektType: String,
    val maaOppgiInntekt: String,
    val maaOppgiInntektFraEgenVirksomhet: String,
    val inntektSkalIkkeOppgis: String,
    val inntektFraEgenVirksomhetSkalIkkeOppgis: String,
    val kanIkkeHaLonnNarKunNorskVirksomhet: String,
)
