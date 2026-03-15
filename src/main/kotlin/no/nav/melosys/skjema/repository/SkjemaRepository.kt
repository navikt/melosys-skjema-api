package no.nav.melosys.skjema.repository

import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.types.common.SkjemaStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SkjemaRepository : JpaRepository<Skjema, UUID> {
    // Utkast-queries for oversikt
    fun findByFnrAndStatus(fnr: String, status: SkjemaStatus): List<Skjema>

    fun findByOpprettetAvAndStatus(
        opprettetAv: String,
        status: SkjemaStatus
    ): List<Skjema>
}
