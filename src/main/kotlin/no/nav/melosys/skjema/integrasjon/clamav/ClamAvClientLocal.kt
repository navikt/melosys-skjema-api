package no.nav.melosys.skjema.integrasjon.clamav

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

private val log = KotlinLogging.logger { }

@Service
@Profile("local-q1 | local-q2")
class ClamAvClientLocal : ClamAvClient {

    override fun scan(fil: MultipartFile) {
        log.info { "Lokal kjøring: Skipper virusscanning av '${fil.originalFilename}'" }
    }
}
