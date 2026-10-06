package no.nav.melosys.skjema.integrasjon.ereg

import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.skjema.inngaarIJuridiskEnhetMedDefaultVerdier
import java.time.LocalDate
import no.nav.melosys.skjema.integrasjon.ereg.dto.Ansatte
import no.nav.melosys.skjema.integrasjon.ereg.dto.Gyldighetsperiode
import no.nav.melosys.skjema.integrasjon.ereg.dto.JuridiskEnhetDetaljer
import no.nav.melosys.skjema.integrasjon.ereg.dto.OrganisasjonDetaljer
import no.nav.melosys.skjema.integrasjon.ereg.dto.toSimpleOrganisasjonDto
import no.nav.melosys.skjema.integrasjon.ereg.exception.OrganisasjonEksistererIkkeException
import no.nav.melosys.skjema.juridiskEnhetMedDefaultVerdier
import no.nav.melosys.skjema.types.felles.OrganisasjonMedJuridiskEnhetDto
import no.nav.melosys.skjema.virksomhetMedDefaultVerdier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EregServiceTest {

    private val eregClient = mockk<EregClient>()
    private val eregService = EregService(eregClient)

    @Test
    fun `hentOrganisasjonMedJuridiskEnhet henter organisasjon og beriker med juridisk enhet`() {
        val juridiskEnhet = juridiskEnhetMedDefaultVerdier().copy(
            organisasjonDetaljer = OrganisasjonDetaljer(ansatte = listOf(Ansatte(antall = 12)))
        )
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
                juridiskEnhet = juridiskEnhet.toSimpleOrganisasjonDto(),
                erOffentligArbeidsgiver = false,
                antallAnsatte = 12
            )
        )
    }

    @Test
    fun `ansattopplysning uten gyldighetsperiode er gjeldende`() {
        val juridiskEnhet = juridiskEnhetMedDefaultVerdier().copy(
            organisasjonDetaljer = OrganisasjonDetaljer(ansatte = listOf(Ansatte(antall = 19)))
        )

        assertThat(juridiskEnhet.antallAnsatte()).isEqualTo(19)
    }

    @Test
    fun `antall ansatte leses fra periode som omfatter dagens dato`() {
        val iDag = LocalDate.now()
        val juridiskEnhet = juridiskEnhetMedDefaultVerdier().copy(
            organisasjonDetaljer = OrganisasjonDetaljer(
                ansatte = listOf(
                    Ansatte(antall = 4, gyldighetsperiode = Gyldighetsperiode(tom = iDag.minusDays(1))),
                    Ansatte(
                        antall = 25,
                        gyldighetsperiode = Gyldighetsperiode(fom = iDag.minusDays(1), tom = iDag.plusDays(1))
                    ),
                    Ansatte(antall = 3, gyldighetsperiode = Gyldighetsperiode(fom = iDag.plusDays(1)))
                )
            )
        )

        assertThat(juridiskEnhet.antallAnsatte()).isEqualTo(25)
    }

    @Test
    fun `juridisk enhet uten registrerte ansatte har 0 ansatte`() {
        assertThat(juridiskEnhetMedDefaultVerdier().antallAnsatte()).isEqualTo(0)
        assertThat(
            juridiskEnhetMedDefaultVerdier().copy(organisasjonDetaljer = OrganisasjonDetaljer(ansatte = emptyList())).antallAnsatte()
        ).isEqualTo(0)
    }

    @Test
    fun `flere gjeldende ansattopplysninger er tvetydig og gir feil`() {
        val juridiskEnhet = juridiskEnhetMedDefaultVerdier().copy(
            organisasjonDetaljer = OrganisasjonDetaljer(ansatte = listOf(Ansatte(antall = 3), Ansatte(antall = 30)))
        )

        assertThrows<IllegalStateException> { juridiskEnhet.antallAnsatte() }
    }

    @Test
    fun `gjeldende ansattopplysning uten antall gir feil`() {
        val juridiskEnhet = juridiskEnhetMedDefaultVerdier().copy(
            organisasjonDetaljer = OrganisasjonDetaljer(ansatte = listOf(Ansatte(antall = null)))
        )

        assertThrows<IllegalStateException> { juridiskEnhet.antallAnsatte() }
    }

    @Test
    fun `klassifiserer juridisk enhet som offentlig bare for STAT og sektorkode 6100`() {
        val offentlig = juridiskEnhetMedDefaultVerdier().copy(
            juridiskEnhetDetaljer = JuridiskEnhetDetaljer(enhetstype = "STAT", sektorkode = "6100")
        )
        val feilSektor = offentlig.copy(
            juridiskEnhetDetaljer = JuridiskEnhetDetaljer(enhetstype = "STAT", sektorkode = "6500")
        )
        val feilEnhetstype = offentlig.copy(
            juridiskEnhetDetaljer = JuridiskEnhetDetaljer(enhetstype = "AS", sektorkode = "6100")
        )

        assertThat(offentlig.erOffentligArbeidsgiver()).isTrue()
        assertThat(feilSektor.erOffentligArbeidsgiver()).isFalse()
        assertThat(feilEnhetstype.erOffentligArbeidsgiver()).isFalse()
        assertThat(juridiskEnhetMedDefaultVerdier().erOffentligArbeidsgiver()).isFalse()
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
