package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.dto.OrganisasjonDto
import no.nav.melosys.skjema.service.AltinnService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/api")
@Protected
class AltinnController(
    private val altinnService: AltinnService
) {

    @GetMapping("/hentTilganger")
    fun hentTilganger(): ResponseEntity<List<OrganisasjonDto>> {
        log.info { "Henter tilganger for innlogget bruker" }

        val tilganger = altinnService.hentBrukersTilganger()

        return ResponseEntity.ok(tilganger)
    }

    @GetMapping("/harTilgang/{orgnr}")
    fun harTilgang(@PathVariable orgnr: String): ResponseEntity<Boolean> {
        log.info { "Sjekker tilgang til organisasjon: $orgnr" }

        if (!orgnr.matches(Regex("\\d{9}"))) {
            log.warn { "Ugyldig organisasjonsnummer: $orgnr" }
            return ResponseEntity.badRequest().build()
        }

        val harTilgang = altinnService.harBrukerTilgang(orgnr)

        return ResponseEntity.ok(harTilgang)
    }
}