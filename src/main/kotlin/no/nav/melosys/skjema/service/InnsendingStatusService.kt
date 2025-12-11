package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.repository.InnsendingRepository
import no.nav.melosys.skjema.repository.SkjemaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

/**
 * Service for transaksjonelle databaseoperasjoner på Innsending.
 *
 * Skilt ut fra InnsendingProsesseringService for å unngå self-invocation
 * problemet med Spring @Transactional.
 */
@Service
class InnsendingStatusService(
    private val innsendingRepository: InnsendingRepository,
    private val skjemaRepository: SkjemaRepository
) {

    /**
     * Oppretter en ny innsending for et skjema med referanseId.
     */
    @Transactional
    fun opprettInnsending(skjema: Skjema, referanseId: String): Innsending {
        val innsending = Innsending(
            skjema = skjema,
            status = InnsendingStatus.MOTTATT,
            referanseId = referanseId
        )
        return innsendingRepository.save(innsending)
    }

    /**
     * Oppdaterer innsendingsstatus og inkrementerer antallForsok.
     */
    @Transactional
    fun oppdaterStatus(
        skjemaId: UUID,
        status: InnsendingStatus,
        feilmelding: String? = null
    ) {
        val innsending = innsendingRepository.findBySkjemaId(skjemaId)
            ?: error("Innsending for skjema $skjemaId ikke funnet")

        innsending.status = status
        innsending.antallForsok += 1
        innsending.sisteForsoekTidspunkt = Instant.now()

        if (feilmelding != null) {
            innsending.feilmelding = feilmelding.take(2000)
        }

        innsendingRepository.save(innsending)
        log.debug { "Oppdatert innsendingStatus til $status for skjema $skjemaId" }
    }

    /**
     * Setter status til UNDER_BEHANDLING for å markere at prosessering er startet.
     * Oppdaterer også sisteForsoekTidspunkt for å kunne detektere "hengende" prosesseringer.
     */
    @Transactional
    fun startProsessering(skjemaId: UUID) {
        val innsending = innsendingRepository.findBySkjemaId(skjemaId)
            ?: error("Innsending for skjema $skjemaId ikke funnet")

        innsending.status = InnsendingStatus.UNDER_BEHANDLING
        innsending.sisteForsoekTidspunkt = Instant.now()
        innsendingRepository.save(innsending)
        log.debug { "Startet prosessering av skjema $skjemaId" }
    }

    /**
     * Oppdaterer journalpostId på skjemaet.
     */
    @Transactional
    fun oppdaterSkjemaJournalpostId(skjemaId: UUID, journalpostId: String) {
        val skjema = skjemaRepository.findById(skjemaId).orElse(null)
            ?: error("Skjema $skjemaId ikke funnet")
        skjema.journalpostId = journalpostId
        skjemaRepository.save(skjema)
    }
}
