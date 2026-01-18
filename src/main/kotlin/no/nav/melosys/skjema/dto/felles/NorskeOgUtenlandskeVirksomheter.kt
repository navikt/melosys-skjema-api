package no.nav.melosys.skjema.dto.felles

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NorskeOgUtenlandskeVirksomheter(
    @field:Valid
    val norskeVirksomheter: List<NorskVirksomhet>?,
    @field:Valid
    val utenlandskeVirksomheter: List<UtenlandskVirksomhet>?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class NorskeOgUtenlandskeVirksomheterMedAnsettelsesform(
    @field:Valid
    val norskeVirksomheter: List<NorskVirksomhet>?,
    @field:Valid
    val utenlandskeVirksomheter: List<UtenlandskVirksomhetMedAnsettelsesform>?
)