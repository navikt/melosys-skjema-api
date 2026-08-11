package no.nav.melosys.skjema.types.utsendtarbeidstaker

import java.util.UUID
import no.nav.melosys.skjema.types.felles.PersonDto
import no.nav.melosys.skjema.types.felles.SimpleOrganisasjonDto

data class OpprettUtsendtArbeidstakerSoknadRequest(
    val representasjonstype: Representasjonstype,
    val radgiverfirma: SimpleOrganisasjonDto?,
    val arbeidsgiver: SimpleOrganisasjonDto,
    val arbeidstaker: PersonDto,
    val opprettetVia: OpprettetVia = OpprettetVia.ORDINAER,
    /**
     * Innsendt arbeidsgiver-del å forhåndsutfylle land og utsendingsperiode fra
     * (motpart-CTA). Må være innlogget brukers egen ventende arbeidsgiver-del —
     * ellers ignoreres den. Verdiene kan fritt overskrives i utfyllingen.
     */
    val prefyllFraSkjemaId: UUID? = null
)
