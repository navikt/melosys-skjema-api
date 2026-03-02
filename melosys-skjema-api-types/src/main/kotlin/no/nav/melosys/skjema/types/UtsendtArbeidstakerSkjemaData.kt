package no.nav.melosys.skjema.types

import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.types.felles.UtsendingsperiodeOgLandDto

interface UtsendtArbeidstakerSkjemaData : SkjemaData {
    val tilleggsopplysninger: TilleggsopplysningerDto?
    val utsendingsperiodeOgLand: UtsendingsperiodeOgLandDto?
}
