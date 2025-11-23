package no.nav.melosys.skjema.integrasjon.pdl.dto

/**
 * Representerer et enkelt innslag i hentPersonBolk-responsen fra PDL
 */
data class PdlPersonBolkEntry(
    /**
     * Identen det ble spurt om (fnr/dnr)
     */
    val ident: String,

    /**
     * Personen med attributtene som ble forespurt.
     * Null hvis person ikke finnes eller ident er ugyldig.
     */
    val person: PdlPerson?,

    /**
     * Statuskode: "ok", "bad_request" eller "not_found"
     */
    val code: String
)
