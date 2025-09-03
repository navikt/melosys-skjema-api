package no.nav.melosys.skjema.service

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.dto.OrganisasjonDto
import no.nav.melosys.skjema.integrasjon.altinn.ArbeidsgiverAltinnTilgangerConsumer
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class AltinnService(
    private val arbeidsgiverAltinnTilgangerConsumer: ArbeidsgiverAltinnTilgangerConsumer
) {

    fun hentBrukersTilganger(): List<OrganisasjonDto> {
        log.info { "Henter brukers tilganger fra Altinn" }

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
            log.error {
                "Feil ved henting av tilganger fra Altinn"
                e
            }
            emptyList()
        }
    }

    fun harBrukerTilgang(orgnr: String): Boolean {
        log.info { "Sjekker om bruker har tilgang til organisasjon: $orgnr" }

        return try {
            arbeidsgiverAltinnTilgangerConsumer.harTilgang(orgnr)
        } catch (e: Exception) {
            log.error {
                "Feil ved sjekk av tilgang til organisasjon $orgnr"
                e
            }
            false
        }
    }
}