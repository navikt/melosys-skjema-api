package no.nav.melosys.skjema.integrasjon.ereg.dto

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.inngaarIJuridiskEnhetMedDefaultVerdier
import no.nav.melosys.skjema.juridiskEnhetMedDefaultVerdier
import no.nav.melosys.skjema.organisasjonsleddMedDefaultVerdier
import no.nav.melosys.skjema.virksomhetMedDefaultVerdier
import org.junit.jupiter.api.Test
import java.time.LocalDate

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

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer traverses alle grener naar foerste ledd er blindvei`() {
        val blindveiLedd = organisasjonsleddMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = null,
            organisasjonsleddOver = null
        )
        val gyldigLedd = organisasjonsleddMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = listOf(
                inngaarIJuridiskEnhetMedDefaultVerdier().copy(organisasjonsnummer = "999888777")
            )
        )

        val virksomhet = virksomhetMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = null,
            bestaarAvOrganisasjonsledd = listOf(
                BestaarAvOrganisasjonsledd(organisasjonsledd = blindveiLedd),
                BestaarAvOrganisasjonsledd(organisasjonsledd = gyldigLedd)
            )
        )

        virksomhet.finnJuridiskEnhetOrganisasjonsnummer().run {
            this.shouldNotBeNull()
            this.shouldBe("999888777")
        }
    }

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer hopper over koblinger uten organisasjonsnummer`() {
        val virksomhet = virksomhetMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = listOf(
                inngaarIJuridiskEnhetMedDefaultVerdier().copy(organisasjonsnummer = null),
                inngaarIJuridiskEnhetMedDefaultVerdier().copy(organisasjonsnummer = "111222333")
            )
        )

        virksomhet.finnJuridiskEnhetOrganisasjonsnummer().run {
            this.shouldNotBeNull()
            this.shouldBe("111222333")
        }
    }

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer foretrekker kobling som er gyldig paa gitt dato`() {
        val virksomhet = virksomhetMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = listOf(
                inngaarIJuridiskEnhetMedDefaultVerdier().copy(
                    organisasjonsnummer = "100000000",
                    gyldighetsperiode = Gyldighetsperiode(
                        fom = LocalDate.of(2000, 1, 1),
                        tom = LocalDate.of(2010, 1, 1)
                    )
                ),
                inngaarIJuridiskEnhetMedDefaultVerdier().copy(
                    organisasjonsnummer = "200000000",
                    gyldighetsperiode = Gyldighetsperiode(fom = LocalDate.of(2011, 1, 1))
                )
            )
        )

        virksomhet.finnJuridiskEnhetOrganisasjonsnummer(idag = LocalDate.of(2026, 6, 9)).run {
            this.shouldNotBeNull()
            this.shouldBe("200000000")
        }
    }

    @Test
    fun `finnJuridiskEnhetOrganisasjonsnummer faller tilbake til foerste kobling naar ingen er gyldige`() {
        val virksomhet = virksomhetMedDefaultVerdier().copy(
            inngaarIJuridiskEnheter = listOf(
                inngaarIJuridiskEnhetMedDefaultVerdier().copy(
                    organisasjonsnummer = "100000000",
                    gyldighetsperiode = Gyldighetsperiode(
                        fom = LocalDate.of(2000, 1, 1),
                        tom = LocalDate.of(2010, 1, 1)
                    )
                )
            )
        )

        virksomhet.finnJuridiskEnhetOrganisasjonsnummer(idag = LocalDate.of(2026, 6, 9)).run {
            this.shouldNotBeNull()
            this.shouldBe("100000000")
        }
    }
}
