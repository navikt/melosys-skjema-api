package no.nav.melosys.skjema.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtenlandskVirksomhet(
    val navn: String,
    val organisasjonsnummer: String?,
    val vegnavnOgHusnummer: String,
    val bygning: String?,
    val postkode: String?,
    val byStedsnavn: String?,
    val region: String?,
    val land: String,
    val tilhorerSammeKonsern: Boolean
)