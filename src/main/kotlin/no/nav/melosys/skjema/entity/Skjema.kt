package no.nav.melosys.skjema.entity

import tools.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

@Entity
@Table(name = "skjema")
class Skjema(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: SkjemaStatus,

    @Column(name = "type", nullable = false)
    val type: String = "A1",

    // Denne burde ikke være nullable lenger
    @Column(name = "fnr", length = 11)
    val fnr: String,

    // Denne burde muligens ligge i
    @Column(name = "orgnr", length = 9)
    val orgnr: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data")
    var data: JsonNode? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    var metadata: JsonNode,

    @Column(name = "opprettet_dato", nullable = false)
    val opprettetDato: Instant = Instant.now(),

    @Column(name = "endret_dato", nullable = false)
    var endretDato: Instant = Instant.now(),

    @Column(name = "opprettet_av", nullable = false, length = 11)
    val opprettetAv: String,

    @Column(name = "endret_av", nullable = false, length = 11)
    var endretAv: String,

    /** Journalpost-ID fra Joark etter vellykket journalføring */
    @Column(name = "journalpost_id")
    var journalpostId: String? = null
)

/**
 * Status for et skjema i søknadsprosessen.
 *
 * Dette er den overordnede statusen som vises til bruker i skjema-web.
 * For detaljert sporing av asynkron prosessering, se [Skjema.innsendingStatus].
 */
enum class SkjemaStatus {
    /** Bruker jobber med søknaden - ikke sendt ennå */
    UTKAST,

    /** Bruker har sendt inn søknaden. Asynkron prosessering pågår eller er fullført. */
    SENDT
}
