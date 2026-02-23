package no.nav.melosys.skjema.repository

import java.util.UUID
import no.nav.melosys.skjema.entity.Vedlegg
import org.springframework.data.jpa.repository.JpaRepository

interface VedleggRepository : JpaRepository<Vedlegg, UUID> {
    fun findBySkjemaId(skjemaId: UUID): List<Vedlegg>
    fun findByIdAndSkjemaId(id: UUID, skjemaId: UUID): Vedlegg?
    fun countBySkjemaId(skjemaId: UUID): Long
}
