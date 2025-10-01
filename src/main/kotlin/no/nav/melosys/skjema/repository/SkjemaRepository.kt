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
}