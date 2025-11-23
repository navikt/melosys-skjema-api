package no.nav.melosys.skjema.repository

import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.entity.SkjemaStatus
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
}