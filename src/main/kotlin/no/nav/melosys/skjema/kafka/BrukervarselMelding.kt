package no.nav.melosys.skjema.kafka

import no.nav.melosys.skjema.types.common.Språk

data class BrukervarselMelding(
    val ident: String,
    val tekster: List<Varseltekst>,
    val link: String? = null,
    val sms: Boolean = true
)

data class Varseltekst(
    val språk: Språk,
    val tekst: String,
    val default: Boolean
)
