package no.nav.melosys.skjema.types.m2m

import no.nav.melosys.skjema.types.common.Saksstatus
import no.nav.melosys.skjema.types.felles.GyldigSaksnummer

data class OppdaterSaksstatusRequest(
    @field:GyldigSaksnummer
    val saksnummer: String,
    val saksstatus: Saksstatus
)
