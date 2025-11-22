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
    fun `verifiserOgHentPerson returnerer navn og fødselsdato når person finnes og etternavn matcher`() {
        val fodselsnummer = "12345678901"
        val etternavn = "Nordmann"
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

        val (navn, fodselsdato) = pdlService.verifiserOgHentPerson(fodselsnummer, etternavn)

        assertThat(navn).isEqualTo("Ola Nordmann")
        assertThat(fodselsdato).isEqualTo(LocalDate.of(1990, 1, 1))
        verify { pdlConsumer.hentPerson(fodselsnummer) }
    }

    @Test
    fun `verifiserOgHentPerson returnerer fullt navn med mellomnavn når person har mellomnavn`() {
        val fodselsnummer = "12345678901"
        val etternavn = "Hansen"
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

        val (navn, fodselsdato) = pdlService.verifiserOgHentPerson(fodselsnummer, etternavn)

        assertThat(navn).isEqualTo("Kari Marie Hansen")
        assertThat(fodselsdato).isEqualTo(LocalDate.of(1985, 5, 15))
    }

    @Test
    fun `verifiserOgHentPerson håndterer etternavn case-insensitive`() {
        val fodselsnummer = "12345678901"
        val etternavn = "NORDMANN" // Store bokstaver
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

        val (navn, _) = pdlService.verifiserOgHentPerson(fodselsnummer, etternavn)

        assertThat(navn).isEqualTo("Ola Nordmann")
    }

    @Test
    fun `verifiserOgHentPerson kaster PersonVerifiseringException når person ikke finnes i PDL`() {
        val fodselsnummer = "12345678901"
        val etternavn = "Nordmann"

        every { pdlConsumer.hentPerson(fodselsnummer) } throws IllegalArgumentException("Fant ikke ident i PDL")

        val exception = assertThrows<PersonVerifiseringException> {
            pdlService.verifiserOgHentPerson(fodselsnummer, etternavn)
        }

        assertThat(exception.message).isEqualTo("Fødselsnummer og etternavn matcher ikke")
    }

    @Test
    fun `verifiserOgHentPerson kaster PersonVerifiseringException når etternavn ikke matcher`() {
        val fodselsnummer = "12345678901"
        val etternavn = "FeilEtternavn"
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
            pdlService.verifiserOgHentPerson(fodselsnummer, etternavn)
        }

        assertThat(exception.message).isEqualTo("Fødselsnummer og etternavn matcher ikke")
    }

    @Test
    fun `verifiserOgHentPerson kaster PersonVerifiseringException når person mangler navn i PDL`() {
        val fodselsnummer = "12345678901"
        val etternavn = "Nordmann"
        val person = PdlPerson(
            navn = emptyList(), // Ingen navn registrert
            foedselsdato = listOf(
                PdlFoedselsdato(foedselsdato = "1990-01-01")
            )
        )

        every { pdlConsumer.hentPerson(fodselsnummer) } returns person

        val exception = assertThrows<PersonVerifiseringException> {
            pdlService.verifiserOgHentPerson(fodselsnummer, etternavn)
        }

        assertThat(exception.message).isEqualTo("Fødselsnummer og etternavn matcher ikke")
    }

    @Test
    fun `verifiserOgHentPerson kaster IllegalArgumentException når person mangler fødselsdato i PDL`() {
        val fodselsnummer = "12345678901"
        val etternavn = "Nordmann"
        val person = PdlPerson(
            navn = listOf(
                PdlNavn(
                    fornavn = "Ola",
                    mellomnavn = null,
                    etternavn = "Nordmann"
                )
            ),
            foedselsdato = emptyList() // Ingen fødselsdato registrert
        )

        every { pdlConsumer.hentPerson(fodselsnummer) } returns person

        val exception = assertThrows<IllegalArgumentException> {
            pdlService.verifiserOgHentPerson(fodselsnummer, etternavn)
        }

        assertThat(exception.message).isEqualTo("Person har ingen fødselsdato registrert i PDL")
    }
}
