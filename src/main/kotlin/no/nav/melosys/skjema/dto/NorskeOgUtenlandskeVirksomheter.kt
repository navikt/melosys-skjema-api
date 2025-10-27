package no.nav.melosys.skjema.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NorskeOgUtenlandskeVirksomheter(
    val norskeVirksomheter: List<NorskVirksomhet>?,
    val utenlandskeVirksomheter: List<UtenlandskVirksomhet>?
)