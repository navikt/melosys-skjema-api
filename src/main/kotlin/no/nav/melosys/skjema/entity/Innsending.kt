package no.nav.melosys.skjema.entity

import jakarta.persistence.*
import no.nav.melosys.skjema.domain.InnsendingStatus
import java.time.Instant
import java.util.*

/**
 * Sporer prosesseringsstatus for innsendte skjemaer.
 *
 * Opprettes når bruker sender inn et skjema, og oppdateres underveis
 * i asynkron prosessering (journalføring, Kafka-sending, varsling).
 *
 * Brukes av [no.nav.melosys.skjema.scheduler.InnsendingRetryScheduler]
 * for å finne feilede innsendinger som skal prøves på nytt.
 */
@Entity
@Table(name = "innsending")
class Innsending(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skjema_id", nullable = false, unique = true)
    val skjema: Skjema,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: InnsendingStatus,

    @Column(name = "feilmelding")
    var feilmelding: String? = null,

    @Column(name = "antall_forsok", nullable = false)
    var antallForsok: Int = 0,

    @Column(name = "siste_forsoek_tidspunkt")
    var sisteForsoekTidspunkt: Instant? = null,

    @Column(name = "opprettet_dato", nullable = false)
    val opprettetDato: Instant = Instant.now()
)
