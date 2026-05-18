package no.nav.melosys.skjema.types.felles

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid

/**
 * Diskriminator-nøkler for virksomheter i blandede norsk/utenlandsk-lister.
 * Brukes både som nøkler i [no.nav.melosys.skjema.types.skjemadefinisjon.ListeFeltDefinisjon.itemTypeLabels]
 * og som type-tag mot frontend.
 */
object VirksomhetTypeKey {
    const val NORSK = "norsk"
    const val UTENLANDSK = "utenlandsk"
}

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