package no.nav.melosys.skjema.types.m2m

import no.nav.melosys.skjema.types.felles.GyldigSaksnummer

data class RegistrerSaksnummerRequest(
    @field:GyldigSaksnummer
    val saksnummer: String
)
