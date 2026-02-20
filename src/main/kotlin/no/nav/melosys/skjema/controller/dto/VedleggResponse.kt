package no.nav.melosys.skjema.controller.dto

import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.entity.Vedlegg
import no.nav.melosys.skjema.entity.VedleggFiltype

data class VedleggResponse(
    val id: UUID,
    val filnavn: String,
    val filtype: VedleggFiltype,
    val filstorrelse: Long,
    val opprettetDato: Instant
) {
    companion object {
        fun fra(vedlegg: Vedlegg) = VedleggResponse(
            id = vedlegg.id,
            filnavn = vedlegg.originalFilnavn,
            filtype = vedlegg.filtype,
            filstorrelse = vedlegg.filstorrelse,
            opprettetDato = vedlegg.opprettetDato
        )
    }
}
