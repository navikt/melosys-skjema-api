package no.nav.melosys.skjema.entity

import com.fasterxml.jackson.databind.JsonNode
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

    @Column(name = "fnr", length = 11)
    val fnr: String? = null,

    @Column(name = "orgnr", length = 9)
    val orgnr: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data")
    var data: JsonNode? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    //TODO: Vurder om vi skal ha dette nonnull, og sørge for å sette riktig metadata-objekt når man lager skjemainstans.
    var metadata: JsonNode? = null,

    @Column(name = "opprettet_dato", nullable = false)
    val opprettetDato: Instant = Instant.now(),

    @Column(name = "endret_dato", nullable = false)
    var endretDato: Instant = Instant.now(),

    @Column(name = "opprettet_av", nullable = false, length = 11)
    val opprettetAv: String,

    @Column(name = "endret_av", nullable = false, length = 11)
    var endretAv: String
)

enum class SkjemaStatus {
    UTKAST,
    SENDT,
    MOTTATT
}