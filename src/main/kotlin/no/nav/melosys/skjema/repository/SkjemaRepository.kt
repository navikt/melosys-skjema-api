package no.nav.melosys.skjema.repository

import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.SkjemaStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
interface SkjemaRepository : JpaRepository<Skjema, UUID> {
    fun findByFnrAndTypeAndStatus(fnr: String, type: SkjemaType, status: SkjemaStatus): List<Skjema>

    fun findByOpprettetAvAndTypeAndStatus(opprettetAv: String, type: SkjemaType, status: SkjemaStatus): List<Skjema>

    @Query("SELECT s FROM Skjema s WHERE s.id = :id AND s.status = 'SENDT'")
    fun findByIdAndStatusSendt(id: UUID): Skjema?

    /**
     * Henter et skjema på id, men ekskluderer soft-deletede (SLETTET) rader.
     *
     * Brukes som standard oppslag i tjenestelaget slik at legacy SLETTET-utkast aldri eksponeres
     * som redigerbare skjema i tidsvinduet før admin-oppryddingen har hard-slettet dem. JPQL mot
     * status-strengen siden [SkjemaStatus.SLETTET] er deprecated legacy og ikke skal brukes i ny kode
     * (fjernes når prod er ryddet, MELOSYS-8157).
     */
    @Query("SELECT s FROM Skjema s WHERE s.id = :id AND s.status != 'SLETTET'")
    fun findAktivById(id: UUID): Skjema?

    fun countByStatus(status: SkjemaStatus): Long

    /**
     * Henter storage-referansene til alle vedlegg som tilhører soft-deletede (SLETTET) skjemaer.
     *
     * MIDLERTIDIG: brukes av admin-oppryddingen som hard-sletter gamle SLETTET-utkast. Native SQL mot
     * status-strengen siden [SkjemaStatus.SLETTET] er deprecated legacy og ikke skal brukes i ny kode
     * (fjernes når prod er ryddet, MELOSYS-8157).
     */
    @Query(
        nativeQuery = true,
        value = """
        SELECT v.storage_referanse
        FROM vedlegg v
        JOIN skjema s ON v.skjema_id = s.id
        WHERE s.status = 'SLETTET'
    """
    )
    fun finnVedleggStorageReferanserForSletteSkjema(): List<String>

    /**
     * Hard-sletter alle soft-deletede (SLETTET) skjemaer. DB-cascade fjerner tilhørende
     * vedlegg-/innsending-/fullmakt-rader. Returnerer antall slettede skjema-rader.
     *
     * MIDLERTIDIG: engangs-opprydding via admin-endepunkt. Fjernes når prod er ryddet (MELOSYS-8157).
     * Egen kort `@Transactional` slik at selve DELETE-en kjøres isolert – kalleren holder ingen
     * DB-transaksjon mens den gjør eksterne bucket-kall.
     */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "DELETE FROM skjema WHERE status = 'SLETTET'")
    fun slettAlleSletteSkjema(): Int
}
