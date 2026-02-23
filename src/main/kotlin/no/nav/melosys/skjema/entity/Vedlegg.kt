package no.nav.melosys.skjema.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import jakarta.persistence.Transient
import java.time.Instant
import java.util.UUID
import no.nav.melosys.skjema.types.vedlegg.VedleggFiltype
import org.springframework.data.domain.Persistable

@Entity
@Table(name = "vedlegg")
class Vedlegg(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skjema_id", nullable = false)
    val skjema: Skjema,

    @Column(name = "filnavn", nullable = false)
    val filnavn: String,

    @Column(name = "original_filnavn", nullable = false)
    val originalFilnavn: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "filtype", nullable = false, length = 20)
    val filtype: VedleggFiltype,

    @Column(name = "filstorrelse", nullable = false)
    val filstorrelse: Long,

    @Column(name = "storage_referanse", nullable = false, length = 500)
    val storageReferanse: String,

    @Column(name = "opprettet_dato", nullable = false)
    val opprettetDato: Instant = Instant.now(),

    @Column(name = "opprettet_av", nullable = false, length = 11)
    val opprettetAv: String
) : Persistable<UUID> {

    @Transient
    private var ny: Boolean = true

    override fun getId(): UUID = id

    override fun isNew(): Boolean = ny

    @PostPersist
    @PostLoad
    fun markerIkkeNy() {
        ny = false
    }
}
