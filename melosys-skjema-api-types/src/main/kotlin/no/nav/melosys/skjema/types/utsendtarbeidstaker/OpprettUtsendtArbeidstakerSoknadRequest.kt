package no.nav.melosys.skjema.types.utsendtarbeidstaker

import no.nav.melosys.skjema.types.felles.PersonDto
import no.nav.melosys.skjema.types.felles.SimpleOrganisasjonDto

data class OpprettUtsendtArbeidstakerSoknadRequest(
    val representasjonstype: Representasjonstype,
    val radgiverfirma: SimpleOrganisasjonDto?,
    val arbeidsgiver: SimpleOrganisasjonDto,
    val arbeidstaker: PersonDto
)
