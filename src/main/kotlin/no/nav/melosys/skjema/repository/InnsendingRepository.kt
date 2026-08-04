package no.nav.melosys.skjema.repository

import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.types.common.Saksstatus
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

    fun findBySkjemaIdIn(skjemaIder: Collection<UUID>): List<Innsending>

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

    /**
     * Henter innsendinger med gitt status og laster tilhørende skjema i samme spørring
     * (JOIN FETCH) for å unngå N+1 når admin-DTO-er mappes – viktig når mange innsendinger
     * har feilet samtidig.
     */
    @Query("SELECT i FROM Innsending i JOIN FETCH i.skjema WHERE i.status = :status")
    fun findByStatusMedSkjema(status: InnsendingStatus): List<Innsending>

    fun countByStatus(status: InnsendingStatus): Long

    /**
     * Henter alle innsendinger med tilhørende innsendte (SENDT) utsendt-arbeidstaker-skjema (JOIN FETCH),
     * for bruksstatistikk regnet i minnet. Gir både innsendingstidspunkt/språk og skjemadata i én spørring.
     */
    @Query(
        "SELECT i FROM Innsending i JOIN FETCH i.skjema s " +
            "WHERE s.status = no.nav.melosys.skjema.types.common.SkjemaStatus.SENDT " +
            "AND s.type = no.nav.melosys.skjema.types.SkjemaType.UTSENDT_ARBEIDSTAKER"
    )
    fun finnAlleInnsendteMedSkjema(): List<Innsending>

    /**
     * Synk-feltene per innsendt skjema, uten å materialisere skjemaets JSONB-data —
     * uttrekket trenger kun radene på innsending-tabellen pluss skjema-id-en (FK).
     */
    @Query(
        "SELECT i.skjema.id AS skjemaId, i.referanseId AS referanseId, i.saksnummer AS saksnummer, " +
            "i.saksstatus AS saksstatus, i.saksstatusOppdatert AS saksstatusOppdatert " +
            "FROM Innsending i " +
            "WHERE i.skjema.status = no.nav.melosys.skjema.types.common.SkjemaStatus.SENDT " +
            "AND i.skjema.type = no.nav.melosys.skjema.types.SkjemaType.UTSENDT_ARBEIDSTAKER"
    )
    fun finnSaksstatusUttrekk(): List<SaksstatusUttrekkProjeksjon>
}

interface SaksstatusUttrekkProjeksjon {
    val skjemaId: UUID
    val referanseId: String
    val saksnummer: String?
    val saksstatus: Saksstatus?
    val saksstatusOppdatert: Instant?
}
