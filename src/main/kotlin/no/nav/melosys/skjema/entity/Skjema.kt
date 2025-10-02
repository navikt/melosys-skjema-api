package no.nav.melosys.skjema.entity

import jakarta.persistence.*
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

    @Column(name = "fnr", nullable = false, length = 11)
    val fnr: String,

    @Column(name = "orgnr", nullable = false, length = 9)
    val orgnr: String,

    @Column(name = "data", columnDefinition = "jsonb")
    var data: String? = null,

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