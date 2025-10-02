package no.nav.melosys.skjema.controller

data class VirksomheterSomUtbetalerLonnOgNaturalytelser(
    val norskeVirksomheter: List<NorskVirksomhet>?,
    val utenlandskeVirksomheter: List<UtenlandskVirksomhet>?
)