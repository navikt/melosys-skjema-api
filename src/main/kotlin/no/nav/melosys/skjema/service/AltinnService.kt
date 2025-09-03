package no.nav.melosys.skjema.service

import no.nav.melosys.skjema.dto.OrganisasjonDto
import no.nav.melosys.skjema.integrasjon.altinn.ArbeidsgiverAltinnTilgangerConsumer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AltinnService(
    private val arbeidsgiverAltinnTilgangerConsumer: ArbeidsgiverAltinnTilgangerConsumer
) {
    
    companion object {
        private val log = LoggerFactory.getLogger(AltinnService::class.java)
    }
    
    fun hentBrukersTilganger(): List<OrganisasjonDto> {
        log.info("Henter brukers tilganger fra Altinn")
        
        return try {
            val tilganger = arbeidsgiverAltinnTilgangerConsumer.hentTilganger()
            
            tilganger.map { org ->
                OrganisasjonDto(
                    orgnr = org.orgnr,
                    navn = org.navn,
                    organisasjonsform = org.organisasjonsform
                )
            }
        } catch (e: Exception) {
            log.error("Feil ved henting av tilganger fra Altinn", e)
            emptyList()
        }
    }
    
    fun harBrukerTilgang(orgnr: String): Boolean {
        log.info("Sjekker om bruker har tilgang til organisasjon: $orgnr")
        
        return try {
            arbeidsgiverAltinnTilgangerConsumer.harTilgang(orgnr)
        } catch (e: Exception) {
            log.error("Feil ved sjekk av tilgang til organisasjon $orgnr", e)
            false
        }
    }
}