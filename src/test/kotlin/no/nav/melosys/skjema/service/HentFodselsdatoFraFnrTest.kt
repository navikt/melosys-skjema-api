package no.nav.melosys.skjema.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.time.LocalDate

class HentFodselsdatoFraFnrTest : FunSpec({

    val service = HentInnsendteSoknaderUtsendtArbeidstakerSkjemaService(
        skjemaRepository = mockk(),
        innsendingRepository = mockk(),
        altinnService = mockk(),
        reprService = mockk(),
        subjectHandler = mockk()
    )

    test("vanlig fødselsnummer gir riktig fødselsdato") {
        // 01.01.1990, individnummer 123
        service.hentFodselsdatoFraFnr("01019012345") shouldBe LocalDate.of(1990, 1, 1)
    }

    test("fødselsnummer med individnummer 000-499 gir århundre 19xx") {
        // 15.06.1985, individnummer 234
        service.hentFodselsdatoFraFnr("15068523456") shouldBe LocalDate.of(1985, 6, 15)
    }

    test("fødselsnummer med individnummer 500-749 og år <= 39 gir århundre 20xx") {
        // 01.03.2010, individnummer 567
        service.hentFodselsdatoFraFnr("01031056789") shouldBe LocalDate.of(2010, 3, 1)
    }

    test("fødselsnummer med individnummer 500-749 og år >= 40 gir århundre 18xx") {
        // 10.05.1854, individnummer 500
        service.hentFodselsdatoFraFnr("10055450012") shouldBe LocalDate.of(1854, 5, 10)
    }

    test("fødselsnummer med individnummer 900-999 og år >= 40 gir århundre 19xx") {
        // 20.12.1940, individnummer 900
        service.hentFodselsdatoFraFnr("20124090012") shouldBe LocalDate.of(1940, 12, 20)
    }

    test("fødselsnummer med individnummer 900-999 og år <= 39 gir århundre 20xx") {
        // 05.08.2020, individnummer 950
        service.hentFodselsdatoFraFnr("05082095012") shouldBe LocalDate.of(2020, 8, 5)
    }

    test("D-nummer justerer dag med -40") {
        // D-nummer: dag 41 = dag 01, 15.03.1990
        service.hentFodselsdatoFraFnr("55039012345") shouldBe LocalDate.of(1990, 3, 15)
    }

    test("D-nummer for dag 01 gir riktig dato") {
        // D-nummer: dag 41 = dag 01, 01.06.1985
        service.hentFodselsdatoFraFnr("41068523456") shouldBe LocalDate.of(1985, 6, 1)
    }

    test("D-nummer for dag 31 gir riktig dato") {
        // D-nummer: dag 71 = dag 31, 31.01.1990
        service.hentFodselsdatoFraFnr("71019012345") shouldBe LocalDate.of(1990, 1, 31)
    }

    test("H-nummer justerer måned med -40") {
        // H-nummer: måned 44 = måned 04, 02.04.1968
        service.hentFodselsdatoFraFnr("02446812345") shouldBe LocalDate.of(1968, 4, 2)
    }

    test("H-nummer for måned 01 gir riktig dato") {
        // H-nummer: måned 41 = måned 01, 10.01.1990
        service.hentFodselsdatoFraFnr("10419012345") shouldBe LocalDate.of(1990, 1, 10)
    }

    test("H-nummer for måned 12 gir riktig dato") {
        // H-nummer: måned 52 = måned 12, 25.12.1975
        service.hentFodselsdatoFraFnr("25527523456") shouldBe LocalDate.of(1975, 12, 25)
    }

    test("FH-nummer justerer måned med -80") {
        // FH-nummer: måned 84 = måned 04, 02.04.1968
        service.hentFodselsdatoFraFnr("02846812345") shouldBe LocalDate.of(1968, 4, 2)
    }

    test("FH-nummer for måned 01 gir riktig dato") {
        // FH-nummer: måned 81 = måned 01, 15.01.1990
        service.hentFodselsdatoFraFnr("15819012345") shouldBe LocalDate.of(1990, 1, 15)
    }

    test("ugyldig fnr med feil lengde kaster IllegalArgumentException") {
        shouldThrow<IllegalArgumentException> {
            service.hentFodselsdatoFraFnr("1234567890")
        }
        shouldThrow<IllegalArgumentException> {
            service.hentFodselsdatoFraFnr("123456789012")
        }
        shouldThrow<IllegalArgumentException> {
            service.hentFodselsdatoFraFnr("")
        }
    }

    test("fnr med ikke-numeriske tegn kaster NumberFormatException") {
        shouldThrow<NumberFormatException> {
            service.hentFodselsdatoFraFnr("abcdefghijk")
        }
    }

    test("individnummer 750-899 med år >= 40 kaster IllegalArgumentException") {
        // Ugyldig kombinasjon
        shouldThrow<IllegalArgumentException> {
            service.hentFodselsdatoFraFnr("01019080012")
        }
    }
})
