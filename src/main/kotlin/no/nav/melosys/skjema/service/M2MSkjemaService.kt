package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.repository.SkjemaRepository
import org.springframework.stereotype.Service
import java.util.UUID

private val log = KotlinLogging.logger { }

@Service
class M2MSkjemaService(
    private val skjemaRepository: SkjemaRepository
) {

    fun hentSkjemaData(id: UUID): Skjema {
        log.info { "Henter skjemadata for id: $id" }
        return skjemaRepository.findById(id)
            .orElseThrow { NoSuchElementException("Skjema med id $id ikke funnet") }
    }
}
