package no.nav.melosys.skjema.repository

import java.util.UUID
import no.nav.melosys.skjema.entity.Vedlegg
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface VedleggRepository : JpaRepository<Vedlegg, UUID> {
    fun findBySkjemaId(skjemaId: UUID): List<Vedlegg>

    /** Henter kun referansene, så ingen Vedlegg-entiteter blir managed før skjemaet slettes. */
    @Query("SELECT v.storageReferanse FROM Vedlegg v WHERE v.skjema.id = :skjemaId")
    fun findStorageReferanserBySkjemaId(skjemaId: UUID): List<String>
    fun findByIdAndSkjemaId(id: UUID, skjemaId: UUID): Vedlegg?
    fun countBySkjemaId(skjemaId: UUID): Long
    fun existsBySkjemaId(skjemaId: UUID): Boolean
}
