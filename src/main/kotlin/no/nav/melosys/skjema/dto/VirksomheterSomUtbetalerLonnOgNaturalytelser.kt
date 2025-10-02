package no.nav.melosys.skjema.dto

data class VirksomheterSomUtbetalerLonnOgNaturalytelser(
    val norskeVirksomheter: List<NorskVirksomhet>?,
    val utenlandskeVirksomheter: List<UtenlandskVirksomhet>?
)