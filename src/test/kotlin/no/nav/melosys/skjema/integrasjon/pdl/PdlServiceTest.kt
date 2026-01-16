package no.nav.melosys.skjema.integrasjon.pdl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlFoedselsdato
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlNavn
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlPerson
import no.nav.melosys.skjema.integrasjon.pdl.exception.PersonVerifiseringException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class PdlServiceTest {

    private val pdlConsumer = mockk<PdlConsumer>()
    private val pdlService = PdlService(pdlConsumer)

    @Test
    fun `verifiserOgHentPerson returnerer navn og fodselsdato naar person finnes og navn matcher`() {
        val fodselsnummer = "12345678901"
        val navn = "Ola Nordmann"
        val person = PdlPerson(
            navn = listOf(
                PdlNavn(
                    fornavn = "Ola",
                    mellomnavn = null,
                    etternavn = "Nordmann"
                )
            ),
            foedselsdato = listOf(
                PdlFoedselsdato(foedselsdato = "1990-01-01")
            )
        )

        every { pdlConsumer.hentPerson(fodselsnummer) } returns person

        val (returnertNavn, fodselsdato) = pdlService.verifiserOgHentPerson(fodselsnummer, navn)

        assertThat(returnertNavn).isEqualTo("Ola Nordmann")
        assertThat(fodselsdato).isEqualTo(LocalDate.of(1990, 1, 1))
        verify { pdlConsumer.hentPerson(fodselsnummer) }
    }

    @Test
    fun `verifiserOgHentPerson returnerer fullt navn med mellomnavn naar person har mellomnavn`() {
        val fodselsnummer = "12345678901"
        val navn = "Kari Marie Hansen"
        val person = PdlPerson(
            navn = listOf(
                PdlNavn(
                    fornavn = "Kari",
                    mellomnavn = "Marie",
                    etternavn = "Hansen"
                )
            ),
            foedselsdato = listOf(
                PdlFoedselsdato(foedselsdato = "1985-05-15")
            )
        )

        every { pdlConsumer.hentPerson(fodselsnummer) } returns person

        val (returnertNavn, fodselsdato) = pdlService.verifiserOgHentPerson(fodselsnummer, navn)

        assertThat(returnertNavn).isEqualTo("Kari Marie Hansen")
        assertThat(fodselsdato).isEqualTo(LocalDate.of(1985, 5, 15))
    }

    @Test
    fun `verifiserOgHentPerson haandterer navn case-insensitive`() {
        val fodselsnummer = "12345678901"
        val navn = "OLA NORDMANN" // Store bokstaver
        val person = PdlPerson(
            navn = listOf(
                PdlNavn(
                    fornavn = "Ola",
                    mellomnavn = null,
                    etternavn = "Nordmann" // Blandet case
                )
            ),
            foedselsdato = listOf(
                PdlFoedselsdato(foedselsdato = "1990-01-01")
            )
        )

        every { pdlConsumer.hentPerson(fodselsnummer) } returns person

        val (returnertNavn, _) = pdlService.verifiserOgHentPerson(fodselsnummer, navn)

        assertThat(returnertNavn).isEqualTo("Ola Nordmann")
    }

    @Test
    fun `verifiserOgHentPerson kaster PersonVerifiseringException naar person ikke finnes i PDL`() {
        val fodselsnummer = "12345678901"
        val navn = "Ola Nordmann"

        every { pdlConsumer.hentPerson(fodselsnummer) } throws IllegalArgumentException("Fant ikke ident i PDL")

        val exception = assertThrows<PersonVerifiseringException> {
            pdlService.verifiserOgHentPerson(fodselsnummer, navn)
        }

        assertThat(exception.message).isEqualTo("Fødselsnummer og navn matcher ikke")
    }

    @Test
    fun `verifiserOgHentPerson kaster PersonVerifiseringException naar navn ikke matcher`() {
        val fodselsnummer = "12345678901"
        val navn = "Feil Navn"
        val person = PdlPerson(
            navn = listOf(
                PdlNavn(
                    fornavn = "Ola",
                    mellomnavn = null,
                    etternavn = "Nordmann"
                )
            ),
            foedselsdato = listOf(
                PdlFoedselsdato(foedselsdato = "1990-01-01")
            )
        )

        every { pdlConsumer.hentPerson(fodselsnummer) } returns person

        val exception = assertThrows<PersonVerifiseringException> {
            pdlService.verifiserOgHentPerson(fodselsnummer, navn)
        }

        assertThat(exception.message).isEqualTo("Fødselsnummer og navn matcher ikke")
    }

    @Test
    fun `verifiserOgHentPerson kaster IllegalArgumentException naar person mangler navn i PDL`() {
        val fodselsnummer = "12345678901"
        val navn = "Ola Nordmann"
        val person = PdlPerson(
            navn = emptyList(), // Ingen navn registrert
            foedselsdato = listOf(
                PdlFoedselsdato(foedselsdato = "1990-01-01")
            )
        )

        every { pdlConsumer.hentPerson(fodselsnummer) } returns person

        val exception = assertThrows<IllegalArgumentException> {
            pdlService.verifiserOgHentPerson(fodselsnummer, navn)
        }

        assertThat(exception.message).isEqualTo("Person har ingen navn registrert i PDL")
    }

    @Test
    fun `verifiserOgHentPerson kaster IllegalArgumentException naar person mangler fodselsdato i PDL`() {
        val fodselsnummer = "12345678901"
        val navn = "Ola Nordmann"
        val person = PdlPerson(
            navn = listOf(
                PdlNavn(
                    fornavn = "Ola",
                    mellomnavn = null,
                    etternavn = "Nordmann"
                )
            ),
            foedselsdato = emptyList() // Ingen fodselsdato registrert
        )

        every { pdlConsumer.hentPerson(fodselsnummer) } returns person

        val exception = assertThrows<IllegalArgumentException> {
            pdlService.verifiserOgHentPerson(fodselsnummer, navn)
        }

        assertThat(exception.message).isEqualTo("Person har ingen fødselsdato registrert i PDL")
    }
}
