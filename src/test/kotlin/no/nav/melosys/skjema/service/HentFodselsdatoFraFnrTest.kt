package no.nav.melosys.skjema.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import org.junit.jupiter.api.Test

class HentFodselsdatoFraFnrTest {

    @Test
    fun `vanlig fødselsnummer gir riktig fødselsdato`() {
        // 01.01.1990, individnummer 123
        hentFodselsdatoFraFnr("01019012345") shouldBe LocalDate.of(1990, 1, 1)
    }

    @Test
    fun `fødselsnummer med individnummer 000-499 gir århundre 19xx`() {
        // 15.06.1985, individnummer 234
        hentFodselsdatoFraFnr("15068523456") shouldBe LocalDate.of(1985, 6, 15)
    }

    @Test
    fun `fødselsnummer med individnummer 500-749 og år lte 39 gir århundre 20xx`() {
        // 01.03.2010, individnummer 567
        hentFodselsdatoFraFnr("01031056789") shouldBe LocalDate.of(2010, 3, 1)
    }

    @Test
    fun `fødselsnummer med individnummer 500-749 og år gte 40 gir århundre 18xx`() {
        // 10.05.1854, individnummer 500
        hentFodselsdatoFraFnr("10055450012") shouldBe LocalDate.of(1854, 5, 10)
    }

    @Test
    fun `fødselsnummer med individnummer 900-999 og år gte 40 gir århundre 19xx`() {
        // 20.12.1940, individnummer 900
        hentFodselsdatoFraFnr("20124090012") shouldBe LocalDate.of(1940, 12, 20)
    }

    @Test
    fun `fødselsnummer med individnummer 900-999 og år lte 39 gir århundre 20xx`() {
        // 05.08.2020, individnummer 950
        hentFodselsdatoFraFnr("05082095012") shouldBe LocalDate.of(2020, 8, 5)
    }

    @Test
    fun `D-nummer justerer dag med -40`() {
        // D-nummer: dag 41 = dag 01, 15.03.1990
        hentFodselsdatoFraFnr("55039012345") shouldBe LocalDate.of(1990, 3, 15)
    }

    @Test
    fun `D-nummer for dag 01 gir riktig dato`() {
        // D-nummer: dag 41 = dag 01, 01.06.1985
        hentFodselsdatoFraFnr("41068523456") shouldBe LocalDate.of(1985, 6, 1)
    }

    @Test
    fun `D-nummer for dag 31 gir riktig dato`() {
        // D-nummer: dag 71 = dag 31, 31.01.1990
        hentFodselsdatoFraFnr("71019012345") shouldBe LocalDate.of(1990, 1, 31)
    }

    @Test
    fun `H-nummer justerer måned med -40`() {
        // H-nummer: måned 44 = måned 04, 02.04.1968
        hentFodselsdatoFraFnr("02446812345") shouldBe LocalDate.of(1968, 4, 2)
    }

    @Test
    fun `H-nummer for måned 01 gir riktig dato`() {
        // H-nummer: måned 41 = måned 01, 10.01.1990
        hentFodselsdatoFraFnr("10419012345") shouldBe LocalDate.of(1990, 1, 10)
    }

    @Test
    fun `H-nummer for måned 12 gir riktig dato`() {
        // H-nummer: måned 52 = måned 12, 25.12.1975
        hentFodselsdatoFraFnr("25527523456") shouldBe LocalDate.of(1975, 12, 25)
    }

    @Test
    fun `FH-nummer justerer måned med -80`() {
        // FH-nummer: måned 84 = måned 04, 02.04.1968
        hentFodselsdatoFraFnr("02846812345") shouldBe LocalDate.of(1968, 4, 2)
    }

    @Test
    fun `FH-nummer for måned 01 gir riktig dato`() {
        // FH-nummer: måned 81 = måned 01, 15.01.1990
        hentFodselsdatoFraFnr("15819012345") shouldBe LocalDate.of(1990, 1, 15)
    }

    @Test
    fun `ugyldig fnr med feil lengde kaster IllegalArgumentException`() {
        shouldThrow<IllegalArgumentException> {
            hentFodselsdatoFraFnr("1234567890")
        }
        shouldThrow<IllegalArgumentException> {
            hentFodselsdatoFraFnr("123456789012")
        }
        shouldThrow<IllegalArgumentException> {
            hentFodselsdatoFraFnr("")
        }
    }

    @Test
    fun `fnr med ikke-numeriske tegn kaster NumberFormatException`() {
        shouldThrow<NumberFormatException> {
            hentFodselsdatoFraFnr("abcdefghijk")
        }
    }

    @Test
    fun `individnummer 750-899 med år gte 40 kaster IllegalArgumentException`() {
        // Ugyldig kombinasjon
        shouldThrow<IllegalArgumentException> {
            hentFodselsdatoFraFnr("01019080012")
        }
    }
}
