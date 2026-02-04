package no.nav.melosys.skjema.integrasjon.ereg.dto

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.inngaarIJuridiskEnhetMedDefaultVerdier
import no.nav.melosys.skjema.juridiskEnhetMedDefaultVerdier
import no.nav.melosys.skjema.organisasjonsleddMedDefaultVerdier
import no.nav.melosys.skjema.virksomhetMedDefaultVerdier
import org.junit.jupiter.api.Test

class OrganisasjonExtensionsTest {

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer returns organisasjonsnummer when organisasjon is JuridiskEnhet`() {
        val juridiskEnhet = juridiskEnhetMedDefaultVerdier()

        juridiskEnhet.finnJuridiskEnhetOrganisasjonsnummer().run {
            this.shouldNotBeNull()
            this.shouldBe(juridiskEnhet.organisasjonsnummer)
        }
    }

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer returns juridisk enhet orgnummer when Virksomhet has inngaarIJuridiskEnheter`() {
        val virksomhet = virksomhetMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = listOf(inngaarIJuridiskEnhetMedDefaultVerdier())
        )

        virksomhet.finnJuridiskEnhetOrganisasjonsnummer().run {
            this.shouldNotBeNull()
            this.shouldBe(virksomhet.inngaarIJuridiskEnheter?.first()?.organisasjonsnummer)
        }
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

        virksomhet.finnJuridiskEnhetOrganisasjonsnummer().run {
            this.shouldNotBeNull()
            this.shouldBe(parentLedd.inngaarIJuridiskEnheter?.first()?.organisasjonsnummer)
        }
    }

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer returns juridisk enhet orgnummer when Organisasjonsledd has inngaarIJuridiskEnheter`() {
        val organisasjonsledd = organisasjonsleddMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = listOf(inngaarIJuridiskEnhetMedDefaultVerdier())
        )

        organisasjonsledd.finnJuridiskEnhetOrganisasjonsnummer().run {
            this.shouldNotBeNull()
            this.shouldBe(organisasjonsledd.inngaarIJuridiskEnheter?.first()?.organisasjonsnummer)
        }
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

        childLedd.finnJuridiskEnhetOrganisasjonsnummer().run {
            this.shouldNotBeNull()
            this.shouldBe(parentLedd.inngaarIJuridiskEnheter?.first()?.organisasjonsnummer)
        }
    }

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer returns null when Virksomhet has no hierarchy`() {
        val virksomhet = virksomhetMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = null,
            bestaarAvOrganisasjonsledd = null
        )

        virksomhet.finnJuridiskEnhetOrganisasjonsnummer().shouldBeNull()
    }

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer returns null when Organisasjonsledd has no hierarchy`() {
        val organisasjonsledd = organisasjonsleddMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = null,
            organisasjonsleddOver = null
        )

        organisasjonsledd.finnJuridiskEnhetOrganisasjonsnummer().shouldBeNull()
    }
}
