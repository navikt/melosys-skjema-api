package no.nav.melosys.skjema.repository

import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.entity.Skjema
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.stereotype.Repository as RepositoryAnnotation

/** Projeksjon for «antall per kategori»-aggregeringer (GROUP BY). */
interface AntallPerKategori {
    val kategori: String?
    val antall: Long
}

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
 * Aggregeringsspørringer for bruksstatistikk på skjema/innsendinger.
 *
 * Skjemadel/flyt/språk hentes via native JSONB-/kolonneaggregering (samme mønster som
 * [UtsendtArbeidstakerSkjemaRepository]). Utkast-tellinger og unike tellinger gjøres med JPQL.
 */
@RepositoryAnnotation
interface AdminStatistikkRepository : Repository<Skjema, UUID> {

    @Query(
        nativeQuery = true,
        value = """
        SELECT metadata->>'skjemadel' AS kategori, COUNT(*) AS antall
        FROM skjema
        WHERE type = 'UTSENDT_ARBEIDSTAKER' AND status = 'SENDT'
        GROUP BY metadata->>'skjemadel'
    """
    )
    fun antallInnsendtPerSkjemadel(): List<AntallPerKategori>

    @Query(
        nativeQuery = true,
        value = """
        SELECT metadata->>'representasjonstype' AS kategori, COUNT(*) AS antall
        FROM skjema
        WHERE type = 'UTSENDT_ARBEIDSTAKER' AND status = 'SENDT'
        GROUP BY metadata->>'representasjonstype'
    """
    )
    fun antallInnsendtPerFlyt(): List<AntallPerKategori>

    @Query(
        nativeQuery = true,
        value = """
        SELECT innsendt_sprak AS kategori, COUNT(*) AS antall
        FROM innsending
        GROUP BY innsendt_sprak
    """
    )
    fun antallInnsendtPerSprak(): List<AntallPerKategori>

    /**
     * Henter alle innsendte (SENDT) utsendt-arbeidstaker-skjema for saksdekningsanalyse i minnet.
     *
     * Saksdekning (begge deler dekket) regnes ut fra faktiske verdier — samme fnr + juridisk enhet +
     * overlappende periode — på samme måte som mottak grupperer relaterte deler. Datamengden er liten
     * og endepunktet kjøres sjelden, så in-memory er ok.
     */
    @Query(
        "SELECT s FROM Skjema s " +
            "WHERE s.status = no.nav.melosys.skjema.types.common.SkjemaStatus.SENDT " +
            "AND s.type = no.nav.melosys.skjema.types.SkjemaType.UTSENDT_ARBEIDSTAKER"
    )
    fun finnAlleInnsendte(): List<Skjema>

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

    @Query("SELECT COUNT(s) FROM Skjema s WHERE s.status = no.nav.melosys.skjema.types.common.SkjemaStatus.SENDT")
    fun antallInnsendteSkjema(): Long

    @Query("SELECT COUNT(DISTINCT s.fnr) FROM Skjema s WHERE s.status = no.nav.melosys.skjema.types.common.SkjemaStatus.SENDT")
    fun antallUnikePersoner(): Long

    @Query("SELECT COUNT(DISTINCT s.orgnr) FROM Skjema s WHERE s.status = no.nav.melosys.skjema.types.common.SkjemaStatus.SENDT")
    fun antallUnikeVirksomheter(): Long

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
