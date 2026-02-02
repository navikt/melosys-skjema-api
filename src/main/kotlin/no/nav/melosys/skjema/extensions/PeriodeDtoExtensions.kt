package no.nav.melosys.skjema.extensions

import no.nav.melosys.skjema.types.felles.PeriodeDto

fun PeriodeDto.overlapper(annenPeriode: PeriodeDto): Boolean =
    this.fraDato <= annenPeriode.tilDato && this.tilDato >= annenPeriode.fraDato
