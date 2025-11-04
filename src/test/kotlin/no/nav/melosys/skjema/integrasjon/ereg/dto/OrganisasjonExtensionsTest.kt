package no.nav.melosys.skjema.integrasjon.ereg.dto

import no.nav.melosys.skjema.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrganisasjonExtensionsTest {

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer returns organisasjonsnummer when organisasjon is JuridiskEnhet`() {
        val juridiskEnhet = juridiskEnhetMedDefaultVerdier()

        val result = juridiskEnhet.finnJuridiskEnhetOrganisasjonsnummer()

        assertThat(result).isEqualTo("123456789")
    }

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer returns juridisk enhet orgnummer when Virksomhet has inngaarIJuridiskEnheter`() {
        val virksomhet = virksomhetMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = listOf(inngaarIJuridiskEnhetMedDefaultVerdier())
        )

        val result = virksomhet.finnJuridiskEnhetOrganisasjonsnummer()

        assertThat(result).isEqualTo("123456789")
    }

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer traverses through bestaarAvOrganisasjonsledd when Virksomhet has no inngaarIJuridiskEnheter`() {
        val parentLedd = organisasjonsleddMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = listOf(inngaarIJuridiskEnhetMedDefaultVerdier())
        )

        val virksomhet = virksomhetMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = null,
            bestaarAvOrganisasjonsledd = listOf(
                BestaarAvOrganisasjonsledd(
                    organisasjonsledd = parentLedd
                )
            )
        )

        val result = virksomhet.finnJuridiskEnhetOrganisasjonsnummer()

        assertThat(result).isEqualTo("123456789")
    }

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer returns juridisk enhet orgnummer when Organisasjonsledd has inngaarIJuridiskEnheter`() {
        val organisasjonsledd = organisasjonsleddMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = listOf(inngaarIJuridiskEnhetMedDefaultVerdier())
        )

        val result = organisasjonsledd.finnJuridiskEnhetOrganisasjonsnummer()

        assertThat(result).isEqualTo("123456789")
    }

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer traverses through organisasjonsleddOver when Organisasjonsledd has no inngaarIJuridiskEnheter`() {
        val parentLedd = organisasjonsleddMedDefaultVerdier().copy(
            organisasjonsnummer = "777777777",
            inngaarIJuridiskEnheter = listOf(inngaarIJuridiskEnhetMedDefaultVerdier())
        )

        val childLedd = organisasjonsleddMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = null,
            organisasjonsleddOver = listOf(
                BestaarAvOrganisasjonsledd(
                    organisasjonsledd = parentLedd
                )
            )
        )

        val result = childLedd.finnJuridiskEnhetOrganisasjonsnummer()

        assertThat(result).isEqualTo("123456789")
    }

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer returns null when Virksomhet has no hierarchy`() {
        val virksomhet = virksomhetMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = null,
            bestaarAvOrganisasjonsledd = null
        )

        val result = virksomhet.finnJuridiskEnhetOrganisasjonsnummer()

        assertThat(result).isNull()
    }

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer returns null when Organisasjonsledd has no hierarchy`() {
        val organisasjonsledd = organisasjonsleddMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = null,
            organisasjonsleddOver = null
        )

        val result = organisasjonsledd.finnJuridiskEnhetOrganisasjonsnummer()

        assertThat(result).isNull()
    }
}
