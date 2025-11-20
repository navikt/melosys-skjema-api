package no.nav.melosys.skjema.integrasjon.ereg

import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.skjema.inngaarIJuridiskEnhetMedDefaultVerdier
import no.nav.melosys.skjema.juridiskEnhetMedDefaultVerdier
import no.nav.melosys.skjema.virksomhetMedDefaultVerdier
import no.nav.melosys.skjema.integrasjon.ereg.dto.OrganisasjonMedJuridiskEnhet
import no.nav.melosys.skjema.integrasjon.ereg.exception.OrganisasjonEksistererIkkeException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EregServiceTest {

    private val eregConsumer = mockk<EregConsumer>()
    private val eregService = EregService(eregConsumer)

    @Test
    fun `hentOrganisasjonMedJuridiskEnhet henter organisasjon og beriker med juridisk enhet`() {
        val juridiskEnhet = juridiskEnhetMedDefaultVerdier()
        val virksomhet = virksomhetMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = listOf(inngaarIJuridiskEnhetMedDefaultVerdier()
                .copy(organisasjonsnummer = juridiskEnhet.organisasjonsnummer
                )
            )
        )

        every { eregConsumer.hentOrganisasjon(virksomhet.organisasjonsnummer, inkluderHierarki = true) } returns virksomhet
        every { eregConsumer.hentOrganisasjon(juridiskEnhet.organisasjonsnummer) } returns juridiskEnhet

        val result = eregService.hentOrganisasjonMedJuridiskEnhet(virksomhet.organisasjonsnummer)

        assertThat(result).isEqualTo(
            OrganisasjonMedJuridiskEnhet(
                organisasjon = virksomhet,
                juridiskEnhet = juridiskEnhet
            )
        )
    }

    @Test
    fun `organisasjonsnummerEksisterer returnerer true når organisasjon eksisterer`() {
        val organisasjon = juridiskEnhetMedDefaultVerdier()

        every { eregConsumer.hentOrganisasjon(organisasjon.organisasjonsnummer) } returns organisasjon

        val result = eregService.organisasjonsnummerEksisterer(organisasjon.organisasjonsnummer)

        assertThat(result).isTrue()
    }

    @Test
    fun `organisasjonsnummerEksisterer returnerer false når organisasjon ikke eksisterer`() {
        val orgnr = "999999999"

        every { eregConsumer.hentOrganisasjon(orgnr) } throws OrganisasjonEksistererIkkeException(orgnr)

        val result = eregService.organisasjonsnummerEksisterer(orgnr)

        assertThat(result).isFalse()
    }

}
