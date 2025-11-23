package no.nav.melosys.skjema.repository

import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SkjemaRepository : JpaRepository<Skjema, UUID> {
    fun findByFnr(fnr: String): List<Skjema>
    fun findByOrgnr(orgnr: String): List<Skjema>
    fun findByStatus(status: SkjemaStatus): List<Skjema>
    fun findByFnrAndOrgnr(fnr: String, orgnr: String): List<Skjema>

    // Ownership-aware methods
    fun findByIdAndFnr(id: UUID, fnr: String): Skjema?
    fun existsByIdAndFnr(id: UUID, fnr: String): Boolean

    // Metadata-based queries (JSONB)
    @Query("""
        SELECT * FROM skjema
        WHERE jsonb_extract_path_text(metadata, 'fullmektigFnr') = :fnr
    """, nativeQuery = true)
    fun findByFullmektigFnr(@Param("fnr") fnr: String): List<Skjema>

    @Query("""
        SELECT * FROM skjema
        WHERE jsonb_extract_path_text(metadata, 'representasjonstype') = :type
    """, nativeQuery = true)
    fun findByRepresentasjonstype(@Param("type") representasjonstype: String): List<Skjema>

    @Query("""
        SELECT * FROM skjema
        WHERE fnr = :fnr
        OR jsonb_extract_path_text(metadata, 'fullmektigFnr') = :fnr
    """, nativeQuery = true)
    fun findByFnrOrFullmektigFnr(@Param("fnr") fnr: String): List<Skjema>

    // Utkast-queries for oversikt
    fun findByFnrAndStatus(fnr: String, status: SkjemaStatus): List<Skjema>

    fun findByOpprettetAvAndStatus(
        opprettetAv: String,
        status: SkjemaStatus
    ): List<Skjema>

    // Innsendte søknader queries - DEG_SELV (arbeidstaker)
    fun findByFnrAndStatusIn(fnr: String, statuses: List<SkjemaStatus>, pageable: Pageable): Page<Skjema>

    // Innsendte søknader queries - ARBEIDSGIVER
    fun findByOrgnrInAndStatusIn(orgnrs: List<String>, statuses: List<SkjemaStatus>, pageable: Pageable): Page<Skjema>

    // Innsendte søknader queries - RADGIVER
    @Query("""
        SELECT s FROM Skjema s
        WHERE s.orgnr IN :orgnrs
        AND s.status IN :statuses
        AND jsonb_extract_path_text(s.metadata, 'radgiverfirma', 'orgnr') = :radgiverfirmaOrgnr
    """)
    fun findInnsendteForRadgiver(
        @Param("orgnrs") orgnrs: List<String>,
        @Param("statuses") statuses: List<SkjemaStatus>,
        @Param("radgiverfirmaOrgnr") radgiverfirmaOrgnr: String,
        pageable: Pageable
    ): Page<Skjema>

    // Innsendte søknader queries - ANNEN_PERSON (fullmektig)
    fun findByFnrInAndStatusIn(fnrs: List<String>, statuses: List<SkjemaStatus>, pageable: Pageable): Page<Skjema>
}