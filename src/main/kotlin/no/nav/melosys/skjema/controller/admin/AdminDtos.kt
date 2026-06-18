package no.nav.melosys.skjema.controller.admin

import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.types.common.SkjemaStatus

/**
 * Administrativ visning av en innsending. Inneholder bevisst ingen personopplysninger
 * (fødselsnummer/navn) – kun orgnr og prosesseringsmetadata.
 */
data class InnsendingAdminDto(
    val innsendingId: UUID,
    val skjemaId: UUID,
    val referanseId: String,
    val status: InnsendingStatus,
    val skjemaStatus: SkjemaStatus,
    val orgnr: String,
    val antallForsok: Int,
    val feilmelding: String?,
    val sisteForsoekTidspunkt: Instant?,
    val opprettetDato: Instant,
    val saksnummer: String?
)

/**
 * Aggregert statistikk for skjema og innsendinger – nyttig som operasjonelt dashbord.
 */
data class AdminStatistikkDto(
    val skjemaPerStatus: Map<SkjemaStatus, Long>,
    val innsendingPerStatus: Map<InnsendingStatus, Long>,
    val antallFeiledeInnsendinger: Long
)

data class AntallDto(
    val antall: Long
)

data class RetryResultatDto(
    val antallForsoekt: Int,
    val antallFeilet: Int
)
