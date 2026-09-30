package no.nav.melosys.skjema.repository

import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.SkjemaStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import jakarta.persistence.LockModeType
import java.util.*

@Repository
interface SkjemaRepository : JpaRepository<Skjema, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Skjema s WHERE s.id = :id")
    fun findByIdForUpdate(id: UUID): Skjema?

    fun findByFnrAndTypeAndStatus(fnr: String, type: SkjemaType, status: SkjemaStatus): List<Skjema>

    fun findByOpprettetAvAndTypeAndStatus(opprettetAv: String, type: SkjemaType, status: SkjemaStatus): List<Skjema>

    @Query("SELECT s FROM Skjema s WHERE s.id = :id AND s.status = 'SENDT'")
    fun findByIdAndStatusSendt(id: UUID): Skjema?

    fun countByStatus(status: SkjemaStatus): Long
}
