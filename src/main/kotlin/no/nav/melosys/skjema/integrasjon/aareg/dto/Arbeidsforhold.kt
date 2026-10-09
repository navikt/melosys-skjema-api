package no.nav.melosys.skjema.integrasjon.aareg.dto

import java.time.LocalDate

/**
 * Utdrag av arbeidsforhold fra AAREG v1 – bare feltene vi trenger for å avgjøre offentlig tjenesteperson.
 */
data class Arbeidsforhold(
    val type: String? = null,
    val arbeidsgiver: Arbeidsgiver? = null,
    val opplysningspliktig: Arbeidsgiver? = null,
    val ansettelsesperiode: Ansettelsesperiode? = null
)

/** Brukes både for arbeidsgiver (underenhet) og opplysningspliktig (juridisk enhet). */
data class Arbeidsgiver(
    val type: String? = null,
    val organisasjonsnummer: String? = null
)

data class Ansettelsesperiode(
    val periode: Periode? = null
)

data class Periode(
    val fom: LocalDate? = null,
    val tom: LocalDate? = null
)
