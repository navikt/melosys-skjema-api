package no.nav.melosys.skjema.dto

data class NorskeOgUtenlandskeVirksomheter(
    val norskeVirksomheter: List<NorskVirksomhet>?,
    val utenlandskeVirksomheter: List<UtenlandskVirksomhet>?
)