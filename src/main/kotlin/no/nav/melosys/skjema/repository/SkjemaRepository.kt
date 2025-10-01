package no.nav.melosys.skjema.repository

import no.nav.melosys.skjema.domain.Skjema
import no.nav.melosys.skjema.domain.SkjemaStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SkjemaRepository : JpaRepository<Skjema, UUID> {
    fun findByFnr(fnr: String): List<Skjema>
    fun findByOrgnr(orgnr: String): List<Skjema>
    fun findByStatus(status: SkjemaStatus): List<Skjema>
    fun findByFnrAndOrgnr(fnr: String, orgnr: String): List<Skjema>
    
    // Ownership-aware methods
    fun findByIdAndFnr(id: UUID, fnr: String): Skjema?
    fun existsByIdAndFnr(id: UUID, fnr: String): Boolean
    fun deleteByIdAndFnr(id: UUID, fnr: String): Int
}