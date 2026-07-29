package no.nav.melosys.skjema.types.utsendtarbeidstaker

import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.types.felles.PeriodeDto

/**
 * En innsendt arbeidsgiver-del som venter på at innlogget bruker sender inn arbeidstakers del.
 * Skal kun inneholde det arbeidstaker trenger for å starte sin del.
 */
data class VentendeMotpartSoknadDto(
    val skjemaId: UUID,
    val arbeidsgiverNavn: String,
    val arbeidsgiverOrgnr: String,
    val utsendingsperiode: PeriodeDto?,
    val innsendtDato: Instant
)

data class VentendeMotpartSoknaderResponse(
    val soknader: List<VentendeMotpartSoknadDto>
)
