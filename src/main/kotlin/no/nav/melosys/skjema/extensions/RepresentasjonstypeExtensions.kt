package no.nav.melosys.skjema.extensions

import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel

fun Representasjonstype.tilSkjemadel(): Skjemadel = when (this) {
    Representasjonstype.DEG_SELV,
    Representasjonstype.ANNEN_PERSON -> Skjemadel.ARBEIDSTAKERS_DEL

    Representasjonstype.ARBEIDSGIVER,
    Representasjonstype.RADGIVER -> Skjemadel.ARBEIDSGIVERS_DEL

    Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT,
    Representasjonstype.RADGIVER_MED_FULLMAKT -> Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
}
