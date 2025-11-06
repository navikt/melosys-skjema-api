package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.integrasjon.ereg.dto.OrganisasjonMedJuridiskEnhet
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/api/ereg")
@Protected
class EregController(
    private val eregService: EregService
) {

    @GetMapping("/organisasjon/{orgnummer}")
    fun hentOrganisasjonMedJuridiskEnhet(
        @PathVariable orgnummer: String,
    ): ResponseEntity<OrganisasjonMedJuridiskEnhet> {
        log.info { "Henter organisasjon fra EREG: $orgnummer" }

        val organisasjon = eregService.hentOrganisasjonMedJuridiskEnhet(orgnummer)

        return ResponseEntity.ok(organisasjon)
    }
}
