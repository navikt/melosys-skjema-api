package no.nav.melosys.skjema.repository

import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.entity.Skjema
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface InnsendingRepository : JpaRepository<Innsending, UUID> {

    fun findBySkjema(skjema: Skjema): Innsending?

    fun findBySkjemaId(skjemaId: UUID): Innsending?

    /**
     * Finner innsendinger som er kandidater for retry.
     *
     * Inkluderer:
     * - MOTTATT eldre enn grensen (aldri startet prosessering)
     * - UNDER_BEHANDLING med gammel/null sisteForsoekTidspunkt (app krasjet under prosessering)
     * - Feilstatuser med færre enn maxAttempts forsøk
     *
     * Race conditions håndteres av ShedLock på scheduleren.
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT i FROM Innsending i
        WHERE (i.status = 'MOTTATT' AND i.opprettetDato < :sisteForsoekTidspunktGrense)
        OR (i.status = 'UNDER_BEHANDLING' AND (i.sisteForsoekTidspunkt IS NULL OR i.sisteForsoekTidspunkt < :sisteForsoekTidspunktGrense))
        OR (i.status = 'KAFKA_FEILET' AND i.antallForsok < :maxAttempts)
    """)
    fun findRetryKandidater(@Param("sisteForsoekTidspunktGrense") sisteForsoekTidspunktGrense: Instant, @Param("maxAttempts") maxAttempts: Int): List<Innsending>

    fun existsByReferanseId(referanseId: String): Boolean
}
