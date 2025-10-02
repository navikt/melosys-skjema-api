package no.nav.melosys.skjema.controller

data class UtenlandskVirksomhet(
    val navn: String,
    val organisasjonsnummer: String,
    val vegnavnOgHusnummer: String,
    val bygning: String?,
    val postkode: String,
    val byStedsnavn: String,
    val region: String,
    val land: String,
    val tilhorerSammeKonsern: Boolean
)