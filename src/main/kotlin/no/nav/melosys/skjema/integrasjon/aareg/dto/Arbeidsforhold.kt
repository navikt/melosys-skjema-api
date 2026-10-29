package no.nav.melosys.skjema.integrasjon.aareg.dto

import java.time.LocalDate

/**
 * Utdrag av arbeidsforhold fra AAREG v2 – feltene vi trenger for å bekrefte arbeidsforholdet.
 */
data class Arbeidsforhold(
    val type: Kodeverksentitet? = null,
    val arbeidssted: Enhet? = null,
    val opplysningspliktig: Enhet? = null,
    val ansettelsesperiode: Ansettelsesperiode? = null
)

data class Kodeverksentitet(
    val kode: String? = null
)

/** Brukes både for arbeidssted (underenhet) og opplysningspliktig (hovedenhet). */
data class Enhet(
    val type: String? = null,
    val identer: List<Ident> = emptyList()
) {
    fun organisasjonsnummer(): String? = identer.firstOrNull { it.type == IDENTTYPE_ORGANISASJONSNUMMER }?.ident
}

data class Ident(
    val type: String? = null,
    val ident: String? = null
)

data class Ansettelsesperiode(
    val startdato: LocalDate? = null,
    val sluttdato: LocalDate? = null
)

const val IDENTTYPE_ORGANISASJONSNUMMER = "ORGANISASJONSNUMMER"
