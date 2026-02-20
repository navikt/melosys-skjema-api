package no.nav.melosys.skjema.types.vedlegg

import java.time.Instant
import java.util.UUID

data class VedleggDto(
    val id: UUID,
    val filnavn: String,
    val filtype: VedleggFiltype,
    val filstorrelse: Long,
    val opprettetDato: Instant
)
