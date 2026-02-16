package no.nav.melosys.skjema.entity

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.config.observability.MDCOperations
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.service.skjemadefinisjon.SpråkConverter
import no.nav.melosys.skjema.types.common.Språk

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
    val opprettetDato: Instant = Instant.now(),

    /** Brukervennlig referanse-ID for søknaden (f.eks. AB12CD) */
    @Column(name = "referanse_id", nullable = false, unique = true)
    val referanseId: String,

    /** Versjon av skjemadefinisjon som ble brukt ved innsending */
    @Column(name = "skjema_definisjon_versjon", nullable = false)
    val skjemaDefinisjonVersjon: String,

    /** Språk som ble brukt ved innsending */
    @Column(name = "innsendt_sprak", nullable = false)
    @Convert(converter = SpråkConverter::class)
    val innsendtSprak: Språk,

    @Column(name = "correlation_id")
    var correlationId: String? = MDCOperations.getCorrelationId(),

    @Column(name = "brukervarsel_sendt", nullable = false)
    var brukervarselSendt: Boolean = false
)
