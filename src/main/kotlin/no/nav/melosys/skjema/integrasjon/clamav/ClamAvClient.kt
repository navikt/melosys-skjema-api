package no.nav.melosys.skjema.integrasjon.clamav

import org.springframework.web.multipart.MultipartFile

interface ClamAvClient {
    fun scan(fil: MultipartFile)
}
