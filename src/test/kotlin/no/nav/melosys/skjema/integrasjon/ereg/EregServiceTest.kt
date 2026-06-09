package no.nav.melosys.skjema.integrasjon.ereg

import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.skjema.inngaarIJuridiskEnhetMedDefaultVerdier
import no.nav.melosys.skjema.integrasjon.ereg.dto.toSimpleOrganisasjonDto
import no.nav.melosys.skjema.integrasjon.ereg.exception.JuridiskEnhetIkkeFunnetException
import no.nav.melosys.skjema.integrasjon.ereg.exception.OrganisasjonEksistererIkkeException
import no.nav.melosys.skjema.juridiskEnhetMedDefaultVerdier
import no.nav.melosys.skjema.types.felles.OrganisasjonMedJuridiskEnhetDto
import no.nav.melosys.skjema.virksomhetMedDefaultVerdier
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
            OrganisasjonMedJuridiskEnhetDto(
                organisasjon = virksomhet.toSimpleOrganisasjonDto(),
                juridiskEnhet = juridiskEnhet.toSimpleOrganisasjonDto()
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

    @Test
    fun `hentOrganisasjonMedJuridiskEnhet kaster JuridiskEnhetIkkeFunnetException naar hierarki mangler juridisk enhet`() {
        val virksomhet = virksomhetMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = null,
            bestaarAvOrganisasjonsledd = null
        )

        every { eregConsumer.hentOrganisasjon(virksomhet.organisasjonsnummer, inkluderHierarki = true) } returns virksomhet

        assertThatThrownBy { eregService.hentOrganisasjonMedJuridiskEnhet(virksomhet.organisasjonsnummer) }
            .isInstanceOf(JuridiskEnhetIkkeFunnetException::class.java)
    }

    @Test
    fun `hentOrganisasjonMedJuridiskEnhet kaster JuridiskEnhetIkkeFunnetException naar oppslag ikke er juridisk enhet`() {
        val juridiskEnhetOrgnr = "100000000"
        val virksomhet = virksomhetMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = listOf(
                inngaarIJuridiskEnhetMedDefaultVerdier().copy(organisasjonsnummer = juridiskEnhetOrgnr)
            )
        )
        // EREG returnerer en Virksomhet (ikke JuridiskEnhet) for det oppslåtte nummeret
        val feilType = virksomhetMedDefaultVerdier().copy(organisasjonsnummer = juridiskEnhetOrgnr)

        every { eregConsumer.hentOrganisasjon(virksomhet.organisasjonsnummer, inkluderHierarki = true) } returns virksomhet
        every { eregConsumer.hentOrganisasjon(juridiskEnhetOrgnr) } returns feilType

        assertThatThrownBy { eregService.hentOrganisasjonMedJuridiskEnhet(virksomhet.organisasjonsnummer) }
            .isInstanceOf(JuridiskEnhetIkkeFunnetException::class.java)
    }

}
