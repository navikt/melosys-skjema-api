package no.nav.melosys.skjema.integrasjon.ereg

import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.skjema.inngaarIJuridiskEnhetMedDefaultVerdier
import no.nav.melosys.skjema.integrasjon.ereg.dto.toSimpleOrganisasjonDto
import no.nav.melosys.skjema.integrasjon.ereg.exception.OrganisasjonEksistererIkkeException
import no.nav.melosys.skjema.juridiskEnhetMedDefaultVerdier
import no.nav.melosys.skjema.types.felles.OrganisasjonMedJuridiskEnhetDto
import no.nav.melosys.skjema.virksomhetMedDefaultVerdier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EregServiceTest {

    private val eregClient = mockk<EregClient>()
    private val eregService = EregService(eregClient)

    @Test
    fun `hentOrganisasjonMedJuridiskEnhet henter organisasjon og beriker med juridisk enhet`() {
        val juridiskEnhet = juridiskEnhetMedDefaultVerdier()
        val virksomhet = virksomhetMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = listOf(inngaarIJuridiskEnhetMedDefaultVerdier()
                .copy(organisasjonsnummer = juridiskEnhet.organisasjonsnummer
                )
            )
        )

        every { eregClient.hentOrganisasjon(virksomhet.organisasjonsnummer, inkluderHierarki = true) } returns virksomhet
        every { eregClient.hentOrganisasjon(juridiskEnhet.organisasjonsnummer) } returns juridiskEnhet

        val result = eregService.hentOrganisasjonMedJuridiskEnhet(virksomhet.organisasjonsnummer)

        assertThat(result).isEqualTo(
            OrganisasjonMedJuridiskEnhetDto(
                organisasjon = virksomhet.toSimpleOrganisasjonDto(),
                juridiskEnhet = juridiskEnhet.toSimpleOrganisasjonDto()
            )
        )
    }

    @Test
    fun `organisasjonsnummerEksisterer returnerer true når organisasjon eksisterer`() {
        val organisasjon = juridiskEnhetMedDefaultVerdier()

        every { eregClient.hentOrganisasjon(organisasjon.organisasjonsnummer) } returns organisasjon

        val result = eregService.organisasjonsnummerEksisterer(organisasjon.organisasjonsnummer)

        assertThat(result).isTrue()
    }

    @Test
    fun `organisasjonsnummerEksisterer returnerer false når organisasjon ikke eksisterer`() {
        val orgnr = "999999999"

        every { eregClient.hentOrganisasjon(orgnr) } throws OrganisasjonEksistererIkkeException(orgnr)

        val result = eregService.organisasjonsnummerEksisterer(orgnr)

        assertThat(result).isFalse()
    }

}
