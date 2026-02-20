package no.nav.melosys.skjema.extensions

import no.nav.melosys.skjema.entity.Vedlegg
import no.nav.melosys.skjema.types.vedlegg.VedleggDto

fun Vedlegg.toVedleggDto() = VedleggDto(
    id = this.id,
    filnavn = this.originalFilnavn,
    filtype = this.filtype,
    filstorrelse = this.filstorrelse,
    opprettetDato = this.opprettetDato
)
