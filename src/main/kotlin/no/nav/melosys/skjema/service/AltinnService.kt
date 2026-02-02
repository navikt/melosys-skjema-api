package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.types.OrganisasjonDto
import no.nav.melosys.skjema.integrasjon.altinn.ArbeidsgiverAltinnTilgangerConsumer
import no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgang
import no.nav.melosys.skjema.integrasjon.altinn.dto.OrganisasjonMedTilgang
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class AltinnService(
    private val arbeidsgiverAltinnTilgangerConsumer: ArbeidsgiverAltinnTilgangerConsumer,
    @param:Value("\${altinn.ressurs}") private val altinnRessurs: String
) {

    fun hentBrukersTilganger(): List<OrganisasjonDto> {
        log.info { "Henter brukers tilganger fra Altinn" }

        return try {
            val response = arbeidsgiverAltinnTilgangerConsumer.hentTilganger()
            
            if (response.isError) {
                log.warn { "Altinn-tilganger returnerte feil-status" }
                return emptyList()
            }

            val organisasjoner = finnOrganisasjonerMedRessurs(response, altinnRessurs)
            
            organisasjoner.map { org ->
                OrganisasjonDto(
                    orgnr = org.orgnr,
                    navn = org.navn,
                    organisasjonsform = org.organisasjonsform
                )
            }
        } catch (e: Exception) {
            log.error { "Feil ved henting av tilganger fra Altinn: ${e.message}" }
            emptyList()
        }
    }

    fun harBrukerTilgang(orgnr: String): Boolean {
        log.info { "Sjekker om bruker har tilgang til organisasjon: $orgnr" }

        return try {
            val tilganger = hentOrganisasjonerMedTilgang()
            tilganger.any { it.orgnr == orgnr }
        } catch (e: Exception) {
            log.error { "Feil ved sjekk av tilgang til organisasjon $orgnr: ${e.message}" }
            false
        }
    }
    
    private fun hentOrganisasjonerMedTilgang(): List<OrganisasjonMedTilgang> {
        val response = arbeidsgiverAltinnTilgangerConsumer.hentTilganger()
        
        if (response.isError) {
            log.warn { "Altinn-tilganger returnerte feil-status" }
            return emptyList()
        }
        
        return finnOrganisasjonerMedRessurs(response, altinnRessurs)
    }

    private fun finnOrganisasjonerMedRessurs(
        response: no.nav.melosys.skjema.integrasjon.altinn.dto.AltinnTilgangerResponse,
        ressurs: String
    ): List<OrganisasjonMedTilgang> {
        val organisasjoner = mutableListOf<OrganisasjonMedTilgang>()

        // Sjekk tilgangTilOrgNr-mappingen for å finne organisasjoner med riktig ressurs
        response.tilgangTilOrgNr[ressurs]?.forEach { orgnr ->
            // Finn organisasjonen i hierarkiet
            finnOrganisasjonIHierarki(response.hierarki, orgnr)?.let { org ->
                organisasjoner.add(
                    OrganisasjonMedTilgang(
                        orgnr = org.orgnr,
                        navn = org.navn,
                        organisasjonsform = org.organisasjonsform
                    )
                )
            }
        }

        return organisasjoner
    }

    private fun finnOrganisasjonIHierarki(
        hierarki: List<AltinnTilgang>,
        orgnr: String
    ): AltinnTilgang? {
        for (org in hierarki) {
            if (org.orgnr == orgnr) {
                return org
            }
            // Søk rekursivt i underenheter
            finnOrganisasjonIHierarki(org.underenheter, orgnr)?.let {
                return it
            }
        }
        return null
    }
}