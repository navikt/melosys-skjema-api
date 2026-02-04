package no.nav.melosys.skjema.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.SkjemaStatus
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import tools.jackson.databind.JsonNode

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

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: SkjemaType = SkjemaType.UTSENDT_ARBEIDSTAKER,

    @Column(name = "fnr", nullable = false, length = 11)
    val fnr: String,

    @Column(name = "orgnr", nullable = false, length = 9)
    val orgnr: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data")
    var data: JsonNode? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false)
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
