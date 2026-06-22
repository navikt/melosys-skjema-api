package no.nav.melosys.skjema.repository

import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.entity.Skjema
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.stereotype.Repository as RepositoryAnnotation

/**
 * Aldersfordeling for utkast, beregnet i én spørring (ett snapshot) slik at bøttene alltid
 * er ikke-negative og summerer til [totalt]. Bøttene er gjensidig utelukkende.
 */
interface UtkastAldersfordeling {
    val totalt: Long
    val under1Dag: Long
    val mellom1Og7Dager: Long
    val mellom7Og30Dager: Long
    val over30Dager: Long
}

/** Innsendingstrend (kumulative vinduer) beregnet i ett snapshot. */
interface InnsendtTrend {
    val sisteDoegn: Long
    val siste7Dager: Long
    val siste30Dager: Long
}

/**
 * Spørringer for utkast og innsendingstrend i bruksstatistikken. Disse er nåtilstand-mål og
 * påvirkes ikke av periodefilteret. Innsendt-statistikken (fordelinger, saksdekning, toppliste)
 * regnes i minnet i [no.nav.melosys.skjema.service.AdminService] for å kunne filtreres på periode.
 */
@RepositoryAnnotation
interface AdminStatistikkRepository : Repository<Skjema, UUID> {

    @Query(
        nativeQuery = true,
        value = """
        SELECT
          COUNT(*) AS "totalt",
          COUNT(*) FILTER (WHERE opprettet_dato >= :grense1d) AS "under1Dag",
          COUNT(*) FILTER (WHERE opprettet_dato < :grense1d AND opprettet_dato >= :grense7d) AS "mellom1Og7Dager",
          COUNT(*) FILTER (WHERE opprettet_dato < :grense7d AND opprettet_dato >= :grense30d) AS "mellom7Og30Dager",
          COUNT(*) FILTER (WHERE opprettet_dato < :grense30d) AS "over30Dager"
        FROM skjema
        WHERE type = 'UTSENDT_ARBEIDSTAKER' AND status = 'UTKAST'
    """
    )
    fun utkastAldersfordeling(grense1d: Instant, grense7d: Instant, grense30d: Instant): UtkastAldersfordeling

    @Query("SELECT MIN(s.opprettetDato) FROM Skjema s WHERE s.status = no.nav.melosys.skjema.types.common.SkjemaStatus.UTKAST")
    fun eldsteUtkastOpprettetDato(): Instant?

    /** Innsendingstrend: kumulativt antall innsendinger i siste døgn / 7 / 30 dager (ett snapshot). */
    @Query(
        nativeQuery = true,
        value = """
        SELECT
          COUNT(*) FILTER (WHERE opprettet_dato >= :grense1d) AS "sisteDoegn",
          COUNT(*) FILTER (WHERE opprettet_dato >= :grense7d) AS "siste7Dager",
          COUNT(*) FILTER (WHERE opprettet_dato >= :grense30d) AS "siste30Dager"
        FROM innsending
    """
    )
    fun innsendtTrend(grense1d: Instant, grense7d: Instant, grense30d: Instant): InnsendtTrend
}
