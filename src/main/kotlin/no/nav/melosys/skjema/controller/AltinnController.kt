package no.nav.melosys.skjema.controller

import no.nav.melosys.skjema.dto.OrganisasjonDto
import no.nav.melosys.skjema.service.AltinnService
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@Protected
class AltinnController(
    private val altinnService: AltinnService
) {
    
    companion object {
        private val log = LoggerFactory.getLogger(AltinnController::class.java)
    }
    
    @GetMapping("/hentTilganger")
    fun hentTilganger(): ResponseEntity<List<OrganisasjonDto>> {
        log.info("Henter tilganger for innlogget bruker")
        
        val tilganger = altinnService.hentBrukersTilganger()
        
        return if (tilganger.isNotEmpty()) {
            ResponseEntity.ok(tilganger)
        } else {
            ResponseEntity.noContent().build()
        }
    }
    
    @GetMapping("/harTilgang/{orgnr}")
    fun harTilgang(@PathVariable orgnr: String): ResponseEntity<Boolean> {
        log.info("Sjekker tilgang til organisasjon: $orgnr")
        
        if (!orgnr.matches(Regex("\\d{9}"))) {
            log.warn("Ugyldig organisasjonsnummer: $orgnr")
            return ResponseEntity.badRequest().build()
        }
        
        val harTilgang = altinnService.harBrukerTilgang(orgnr)
        
        return ResponseEntity.ok(harTilgang)
    }
}