package no.nav.melosys.skjema.integrasjon.ereg

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.ereg.dto.JuridiskEnhet
import no.nav.melosys.skjema.integrasjon.ereg.dto.Organisasjon
import no.nav.melosys.skjema.integrasjon.ereg.dto.OrganisasjonMedJuridiskEnhet
import no.nav.melosys.skjema.integrasjon.ereg.dto.finnJuridiskEnhetOrganisasjonsnummer
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class EregService(
    private val eregConsumer: EregConsumer
) {

    fun hentOrganisasjonMedJuridiskEnhet(orgnummer: String): OrganisasjonMedJuridiskEnhet {
        log.info { "Henter organisasjon fra EREG: $orgnummer" }
        val organisasjon = eregConsumer.hentOrganisasjon(orgnummer, inkluderHierarki = true)
        val juridiskEnhet = hentJuridiskEnhetForOrganisasjon(organisasjon)

        return OrganisasjonMedJuridiskEnhet(
            organisasjon = organisasjon,
            juridiskEnhet = juridiskEnhet
        )
    }

    private fun hentJuridiskEnhetForOrganisasjon(organisasjon: Organisasjon): JuridiskEnhet {
        if (organisasjon is JuridiskEnhet) {
            return organisasjon
        }

        val juridiskEnhetOrganisasjonsnummer = organisasjon.finnJuridiskEnhetOrganisasjonsnummer()
            ?: error("Fant ikke juridisk enhet for organisasjon ${organisasjon.organisasjonsnummer}")

        return eregConsumer.hentOrganisasjon(juridiskEnhetOrganisasjonsnummer) as JuridiskEnhet
    }
}
