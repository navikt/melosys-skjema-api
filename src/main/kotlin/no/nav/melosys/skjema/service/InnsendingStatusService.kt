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
     * Oppretter en ny innsending for et skjema.
     */
    @Transactional
    fun opprettInnsending(skjema: Skjema): Innsending {
        val innsending = Innsending(
            skjema = skjema,
            status = InnsendingStatus.MOTTATT
        )
        return innsendingRepository.save(innsending)
    }

    /**
     * Oppdaterer innsendingsstatus og inkrementerer antallForsok.
     */
    @Transactional
    fun oppdaterStatus(
        skjema: Skjema,
        status: InnsendingStatus,
        feilmelding: String? = null
    ) {
        val innsending = innsendingRepository.findBySkjema(skjema)
            ?: throw IllegalArgumentException("Innsending for skjema ${skjema.id} ikke funnet")

        innsending.status = status
        innsending.antallForsok += 1
        innsending.sisteForsoek = Instant.now()

        if (feilmelding != null) {
            innsending.feilmelding = feilmelding
        }

        innsendingRepository.save(innsending)
        log.debug { "Oppdatert innsendingStatus til $status for skjema ${skjema.id}" }
    }

    /**
     * Oppdaterer innsendingsstatus uten å inkrementere antallForsok.
     */
    @Transactional
    fun oppdaterStatusUtenInkrementForsok(skjema: Skjema, status: InnsendingStatus) {
        val innsending = innsendingRepository.findBySkjema(skjema)
            ?: throw IllegalArgumentException("Innsending for skjema ${skjema.id} ikke funnet")

        innsending.status = status
        innsendingRepository.save(innsending)
        log.debug { "Oppdatert innsendingStatus til $status for skjema ${skjema.id}" }
    }

    /**
     * Oppdaterer journalpostId på skjemaet.
     */
    @Transactional
    fun oppdaterSkjemaJournalpostId(skjema: Skjema, journalpostId: String) {
        skjema.journalpostId = journalpostId
        skjemaRepository.save(skjema)
    }
}
