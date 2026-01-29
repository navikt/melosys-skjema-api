package no.nav.melosys.skjema.integrasjon.ereg

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.ereg.dto.JuridiskEnhet
import no.nav.melosys.skjema.integrasjon.ereg.dto.Organisasjon
import no.nav.melosys.skjema.dto.OrganisasjonMedJuridiskEnhetDto
import no.nav.melosys.skjema.dto.SimpleOrganisasjonDto
import no.nav.melosys.skjema.integrasjon.ereg.dto.finnJuridiskEnhetOrganisasjonsnummer
import no.nav.melosys.skjema.integrasjon.ereg.dto.toSimpleOrganisasjonDto
import no.nav.melosys.skjema.integrasjon.ereg.exception.OrganisasjonEksistererIkkeException
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class EregService(
    private val eregConsumer: EregConsumer
) {

    /**
     * Henter organisasjon fra EREG uten hierarki.
     *
     * @param orgnummer 9-sifret organisasjonsnummer
     * @return Organisasjon
     */
    fun hentOrganisasjon(orgnummer: String): SimpleOrganisasjonDto {
        log.info { "Henter organisasjon fra EREG (uten hierarki): ${orgnummer.take(3)}***" }
        return eregConsumer.hentOrganisasjon(orgnummer, inkluderHierarki = false).toSimpleOrganisasjonDto()
    }

    /**
     * Henter organisasjon med juridisk enhet fra EREG.
     *
     * @param orgnummer 9-sifret organisasjonsnummer
     * @return Organisasjon med juridisk enhet
     */
    fun hentOrganisasjonMedJuridiskEnhet(orgnummer: String): OrganisasjonMedJuridiskEnhetDto {
        log.info { "Henter organisasjon fra EREG: ${orgnummer.take(3)}***" }

        val organisasjon = eregConsumer.hentOrganisasjon(orgnummer, inkluderHierarki = true)
        val juridiskEnhet = hentJuridiskEnhetForOrganisasjon(organisasjon)

        return OrganisasjonMedJuridiskEnhetDto(
            organisasjon = organisasjon.toSimpleOrganisasjonDto(),
            juridiskEnhet = juridiskEnhet.toSimpleOrganisasjonDto()
        )
    }

    fun organisasjonsnummerEksisterer(organisasjonsnummer: String): Boolean {
        return try {
            eregConsumer.hentOrganisasjon(organisasjonsnummer)
            true
        } catch (_: OrganisasjonEksistererIkkeException) {
            false
        }
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
