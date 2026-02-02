package no.nav.melosys.skjema.types.felles

import com.fasterxml.jackson.annotation.JsonInclude

interface UtenlandskVirksomhetBase {
    val navn: String
    val organisasjonsnummer: String?
    val vegnavnOgHusnummer: String
    val bygning: String?
    val postkode: String?
    val byStedsnavn: String?
    val region: String?
    val land: String
    val tilhorerSammeKonsern: Boolean
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtenlandskVirksomhet(
    override val navn: String,
    override val organisasjonsnummer: String?,
    override val vegnavnOgHusnummer: String,
    override val bygning: String?,
    override val postkode: String?,
    override val byStedsnavn: String?,
    override val region: String?,
    override val land: String,
    override val tilhorerSammeKonsern: Boolean
) : UtenlandskVirksomhetBase

enum class Ansettelsesform {
    ARBEIDSTAKER_ELLER_FRILANSER,
    SELVSTENDIG_NAERINGSDRIVENDE,
    STATSANSATT
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UtenlandskVirksomhetMedAnsettelsesform(
    override val navn: String,
    override val organisasjonsnummer: String?,
    override val vegnavnOgHusnummer: String,
    override val bygning: String?,
    override val postkode: String?,
    override val byStedsnavn: String?,
    override val region: String?,
    override val land: String,
    override val tilhorerSammeKonsern: Boolean,
    val ansettelsesform: Ansettelsesform
) : UtenlandskVirksomhetBase