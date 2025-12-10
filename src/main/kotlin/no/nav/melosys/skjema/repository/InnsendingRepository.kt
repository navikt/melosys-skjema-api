package no.nav.melosys.skjema.repository

import no.nav.melosys.skjema.entity.Innsending
import no.nav.melosys.skjema.entity.Skjema
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Repository
interface InnsendingRepository : JpaRepository<Innsending, UUID> {

    fun findBySkjema(skjema: Skjema): Innsending?

    /**
     * Finner innsendinger som er kandidater for retry.
     *
     * Inkluderer:
     * - Innsendinger med status MOTTATT som er eldre enn grensen (aldri startet, eller app krasjet)
     * - Innsendinger med feilstatus (JOURNALFORING_FEILET, KAFKA_FEILET) med færre enn maxAttempts forsøk
     */
    @Transactional(readOnly = true)
    @Query("""
        SELECT i FROM Innsending i
        WHERE (i.status = 'MOTTATT' AND i.opprettetDato < :grense)
        OR (i.status IN ('JOURNALFORING_FEILET', 'KAFKA_FEILET') AND i.antallForsok < :maxAttempts)
    """)
    fun findRetryKandidater(@Param("grense") grense: Instant, @Param("maxAttempts") maxAttempts: Int): List<Innsending>
}
