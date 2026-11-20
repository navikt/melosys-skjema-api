package no.nav.melosys.skjema.translations.dto

data class FellesTranslation(
    val organisasjonsnummerHarUgyldigFormat: String,
    val organisasjonsnummerFinnesIkke: String,
    val utenlandskOrganisasjonsnummerErForLangt: String,
    val postkodeErForLang: String,
    val ugyldigFodselsellerDNummer: String,
    val feltErPaakrevd: String
)
