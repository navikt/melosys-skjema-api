package no.nav.melosys.skjema.repository

import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.SkjemaStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SkjemaRepository : JpaRepository<Skjema, UUID> {
    fun findByFnrAndTypeAndStatus(fnr: String, type: SkjemaType, status: SkjemaStatus): List<Skjema>

    fun findByOpprettetAvAndTypeAndStatus(opprettetAv: String, type: SkjemaType, status: SkjemaStatus): List<Skjema>

    @Query("SELECT s FROM Skjema s WHERE s.id = :id AND s.status = 'SENDT'")
    fun findByIdAndStatusSendt(id: UUID): Skjema?

    fun countByStatus(status: SkjemaStatus): Long

    /**
     * Henter storage-referansene til alle vedlegg som tilhører soft-deletede (SLETTET) skjemaer.
     *
     * MIDLERTIDIG: brukes av admin-oppryddingen som hard-sletter gamle SLETTET-utkast. SLETTET er
     * fjernet fra [SkjemaStatus]-enumet, så radene må adresseres med native SQL mot status-strengen.
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
     * MIDLERTIDIG: engangs-opprydding via admin-endepunkt. Fjernes når prod er ryddet.
     */
    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM skjema WHERE status = 'SLETTET'")
    fun slettAlleSletteSkjema(): Int
}
