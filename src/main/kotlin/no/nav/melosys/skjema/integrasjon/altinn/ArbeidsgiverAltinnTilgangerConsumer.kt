package no.nav.melosys.skjema.integrasjon.altinn

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.altinn.dto.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

private val log = KotlinLogging.logger { }

@Component
class ArbeidsgiverAltinnTilgangerConsumer(
    private val arbeidsgiverAltinnTilgangerClient: WebClient,
    @param:Value("\${altinn.ressurs}") private val altinnRolle: String,
) {

    fun hentTilganger(filter: Filter? = null): List<OrganisasjonMedTilgang> {
        log.info { "Henter Altinn-tilganger med filter: $filter" }

        val request = AltinnTilgangerRequest(filter)

        return try {
            val response = arbeidsgiverAltinnTilgangerClient.post()
                .uri("/altinn-tilganger")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AltinnTilgangerResponse::class.java)
                .block()

            if (response == null || response.isError) {
                log.warn { "Altinn-tilganger returnerte feil-status eller null" }
                return emptyList()
            }

            // Finn alle organisasjoner som har den spesifiserte rollen
            finnOrganisasjonerMedRolle(response, altinnRolle)
        } catch (e: WebClientResponseException) {
            log.error {
                "HTTP-feil ved henting av Altinn-tilganger: Status=${e.statusCode}, Body=${e.responseBodyAsString}"
                e
            }
            emptyList()
        } catch (e: Exception) {
            log.error {
                "Uventet feil ved henting av Altinn-tilganger: ${e.message}"
                e
            }
            emptyList()
        }
    }

    fun harTilgang(orgnr: String): Boolean {
        log.info { "Sjekker tilgang til organisasjon: $orgnr" }

        val tilganger = hentTilganger()
        return tilganger.any { it.orgnr == orgnr }
    }

    private fun finnOrganisasjonerMedRolle(
        response: AltinnTilgangerResponse,
        rolle: String
    ): List<OrganisasjonMedTilgang> {
        val organisasjoner = mutableListOf<OrganisasjonMedTilgang>()

        // Sjekk tilgangTilOrgNr-mappingen for å finne organisasjoner med riktig rolle
        response.tilgangTilOrgNr[rolle]?.forEach { orgnr ->
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