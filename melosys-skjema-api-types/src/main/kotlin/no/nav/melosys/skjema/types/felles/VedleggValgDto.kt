package no.nav.melosys.skjema.types.felles

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VedleggValgDto(
    val harAnnenDokumentasjon: Boolean
)
