package no.nav.melosys.skjema.repository

import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.SkjemaStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SkjemaRepository : JpaRepository<Skjema, UUID> {
    fun findByFnrAndTypeAndStatus(fnr: String, type: SkjemaType, status: SkjemaStatus): List<Skjema>

    fun findByOpprettetAvAndTypeAndStatus(opprettetAv: String, type: SkjemaType, status: SkjemaStatus): List<Skjema>

    @Query("SELECT s FROM Skjema s WHERE s.id = :id AND s.status != 'SLETTET'")
    fun findAktivById(id: UUID): Skjema?
}
