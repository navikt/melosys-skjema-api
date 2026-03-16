package no.nav.melosys.skjema.repository

import no.nav.melosys.skjema.entity.Skjema
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

private const val TYPE_UTSENDT_ARBEIDSTAKER = "UTSENDT_ARBEIDSTAKER"

/**
 * Repository for innsendte søknader-queries spesifikke for Utsendt Arbeidstaker.
 *
 * Alle queries filtrerer på type = UTSENDT_ARBEIDSTAKER og representasjonstype i JSONB metadata-feltet
 * for å sikre at kun skjemaer med riktig skjematype og representasjonskontekst returneres.
 */
@Repository
interface UtsendtArbeidstakerSkjemaRepository : JpaRepository<Skjema, UUID> {

    // DEG_SELV (arbeidstaker)
    @Query(
        """
        SELECT * FROM skjema
        WHERE fnr = :fnr
        AND type = '$TYPE_UTSENDT_ARBEIDSTAKER'
        AND status = :status
        AND metadata->>'representasjonstype' = :representasjonstype
    """, nativeQuery = true
    )
    fun findByFnrAndStatusAndRepresentasjonstype(
        @Param("fnr") fnr: String,
        @Param("status") status: String,
        @Param("representasjonstype") representasjonstype: String,
        pageable: Pageable
    ): Page<Skjema>

    @Query(
        """
        SELECT * FROM skjema
        WHERE fnr = :fnr
        AND type = '$TYPE_UTSENDT_ARBEIDSTAKER'
        AND status = :status
        AND metadata->>'representasjonstype' = :representasjonstype
        AND (LOWER(orgnr) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """, nativeQuery = true
    )
    fun findByFnrAndStatusAndRepresentasjonstypeWithSearch(
        @Param("fnr") fnr: String,
        @Param("status") status: String,
        @Param("representasjonstype") representasjonstype: String,
        @Param("searchTerm") searchTerm: String,
        pageable: Pageable
    ): Page<Skjema>

    // ARBEIDSGIVER (inkl. MED_FULLMAKT)
    @Query(
        """
        SELECT * FROM skjema
        WHERE orgnr IN :orgnrs
        AND type = '$TYPE_UTSENDT_ARBEIDSTAKER'
        AND status = :status
        AND metadata->>'representasjonstype' IN :representasjonstyper
    """, nativeQuery = true
    )
    fun findByOrgnrInAndStatusAndRepresentasjonstyper(
        @Param("orgnrs") orgnrs: List<String>,
        @Param("status") status: String,
        @Param("representasjonstyper") representasjonstyper: List<String>,
        pageable: Pageable
    ): Page<Skjema>

    @Query(
        """
        SELECT * FROM skjema
        WHERE orgnr IN :orgnrs
        AND type = '$TYPE_UTSENDT_ARBEIDSTAKER'
        AND status = :status
        AND metadata->>'representasjonstype' IN :representasjonstyper
        AND (LOWER(fnr) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
             OR LOWER(orgnr) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """, nativeQuery = true
    )
    fun findByOrgnrInAndStatusAndRepresentasjonstyperWithSearch(
        @Param("orgnrs") orgnrs: List<String>,
        @Param("status") status: String,
        @Param("representasjonstyper") representasjonstyper: List<String>,
        @Param("searchTerm") searchTerm: String,
        pageable: Pageable
    ): Page<Skjema>

    // RADGIVER (inkl. MED_FULLMAKT)
    @Query(
        """
        SELECT * FROM skjema
        WHERE orgnr IN :orgnrs
        AND type = '$TYPE_UTSENDT_ARBEIDSTAKER'
        AND status = :status
        AND metadata->>'representasjonstype' IN :representasjonstyper
        AND jsonb_extract_path_text(metadata, 'radgiverfirma', 'orgnr') = :radgiverfirmaOrgnr
    """, nativeQuery = true
    )
    fun findInnsendteForRadgiver(
        @Param("orgnrs") orgnrs: List<String>,
        @Param("status") status: String,
        @Param("representasjonstyper") representasjonstyper: List<String>,
        @Param("radgiverfirmaOrgnr") radgiverfirmaOrgnr: String,
        pageable: Pageable
    ): Page<Skjema>

    @Query(
        """
        SELECT * FROM skjema
        WHERE orgnr IN :orgnrs
        AND type = '$TYPE_UTSENDT_ARBEIDSTAKER'
        AND status = :status
        AND metadata->>'representasjonstype' IN :representasjonstyper
        AND jsonb_extract_path_text(metadata, 'radgiverfirma', 'orgnr') = :radgiverfirmaOrgnr
        AND (LOWER(fnr) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
             OR LOWER(orgnr) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """, nativeQuery = true
    )
    fun findInnsendteForRadgiverWithSearch(
        @Param("orgnrs") orgnrs: List<String>,
        @Param("status") status: String,
        @Param("representasjonstyper") representasjonstyper: List<String>,
        @Param("radgiverfirmaOrgnr") radgiverfirmaOrgnr: String,
        @Param("searchTerm") searchTerm: String,
        pageable: Pageable
    ): Page<Skjema>

    // ANNEN_PERSON (fullmektig)
    @Query(
        """
        SELECT * FROM skjema
        WHERE fnr IN :fnrs
        AND type = '$TYPE_UTSENDT_ARBEIDSTAKER'
        AND status = :status
        AND metadata->>'representasjonstype' = :representasjonstype
    """, nativeQuery = true
    )
    fun findByFnrInAndStatusAndRepresentasjonstype(
        @Param("fnrs") fnrs: List<String>,
        @Param("status") status: String,
        @Param("representasjonstype") representasjonstype: String,
        pageable: Pageable
    ): Page<Skjema>

    @Query(
        """
        SELECT * FROM skjema
        WHERE fnr IN :fnrs
        AND type = '$TYPE_UTSENDT_ARBEIDSTAKER'
        AND status = :status
        AND metadata->>'representasjonstype' = :representasjonstype
        AND (LOWER(orgnr) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
    """, nativeQuery = true
    )
    fun findByFnrInAndStatusAndRepresentasjonstypeWithSearch(
        @Param("fnrs") fnrs: List<String>,
        @Param("status") status: String,
        @Param("representasjonstype") representasjonstype: String,
        @Param("searchTerm") searchTerm: String,
        pageable: Pageable
    ): Page<Skjema>
}
