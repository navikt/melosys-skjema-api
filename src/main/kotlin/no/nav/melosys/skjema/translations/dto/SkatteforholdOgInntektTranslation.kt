package no.nav.melosys.skjema.translations.dto

data class SkatteforholdOgInntektTranslation(
    val maaOppgiLandSomUtbetalerPengestotte: String,
    val maaOppgiBelopPengestotte: String,
    val maaOppgiBeskrivelsePengestotte: String,
    val ugyldigBelopFormat: String,
    val maaVelgeMinsteEnArbeidsinntektKilde: String,
    val maaVelgeMinsteEnInntektType: String,
    val maaOppgiInntekterFraUtenlandskVirksomhet: String,
    val maaOppgiInntekterFraEgenVirksomhet: String,
    val inntekterFraUtenlandskVirksomhetSkalIkkeOppgis: String,
    val inntekterFraEgenVirksomhetSkalIkkeOppgis: String,
    val kannIkkeHaLonnNarKunNorskVirksomhet: String,
)
