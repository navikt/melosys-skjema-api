package no.nav.melosys.skjema.integrasjon.repr

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt

class ReprServiceTest : FunSpec({

    val mockConsumer = mockk<ReprConsumer>()
    val service = ReprService(mockConsumer)

    beforeEach {
        io.mockk.clearAllMocks()
    }

    test("hentKanRepresentere skal returnere kun fullmakter med MED-området") {
        val fullmakter = listOf(
            // Fullmakt 1: har MED i både lese- og skriverettigheter
            Fullmakt(
                fullmaktsgiver = "12345678901",
                fullmektig = "98765432109",
                leserettigheter = listOf("MED", "DAG"),
                skriverettigheter = listOf("MED")
            ),
            // Fullmakt 2: har IKKE MED (skal filtreres bort)
            Fullmakt(
                fullmaktsgiver = "22222222222",
                fullmektig = "98765432109",
                leserettigheter = listOf("DAG", "FOS"),
                skriverettigheter = listOf("DAG")
            ),
            // Fullmakt 3: har MED kun i leserettigheter
            Fullmakt(
                fullmaktsgiver = "33333333333",
                fullmektig = "98765432109",
                leserettigheter = listOf("MED"),
                skriverettigheter = emptyList()
            )
        )

        every { mockConsumer.hentKanRepresentere() } returns fullmakter

        val result = service.hentKanRepresentere()

        result.shouldHaveSize(2)
        result[0].fullmaktsgiver shouldBe "12345678901"
        result[1].fullmaktsgiver shouldBe "33333333333"
        verify(exactly = 1) { mockConsumer.hentKanRepresentere() }
    }

    test("hentKanRepresentere skal returnere tom liste når ingen MED-fullmakter") {
        val fullmakter = listOf(
            Fullmakt(
                fullmaktsgiver = "12345678901",
                fullmektig = "98765432109",
                leserettigheter = listOf("DAG", "FOS"),
                skriverettigheter = listOf("DAG")
            ),
            Fullmakt(
                fullmaktsgiver = "22222222222",
                fullmektig = "98765432109",
                leserettigheter = listOf("PEN"),
                skriverettigheter = emptyList()
            )
        )

        every { mockConsumer.hentKanRepresentere() } returns fullmakter

        val result = service.hentKanRepresentere()

        result.shouldBeEmpty()
    }

    test("hentKanRepresentere skal returnere tom liste når consumer returnerer tom liste") {
        every { mockConsumer.hentKanRepresentere() } returns emptyList()

        val result = service.hentKanRepresentere()

        result.shouldBeEmpty()
    }

    test("hentKanRepresentere skal kaste exception ved feil fra consumer") {
        every { mockConsumer.hentKanRepresentere() } throws RuntimeException("API feil")

        val exception = runCatching { service.hentKanRepresentere() }.exceptionOrNull()

        exception shouldBe io.kotest.matchers.types.instanceOf<RuntimeException>()
        exception?.message shouldBe "Kunne ikke hente fullmakter fra repr-api"
    }

    test("harSkriverettigheterForMedlemskap skal returnere true når fullmakt finnes") {
        val fullmakter = listOf(
            Fullmakt(
                fullmaktsgiver = "12345678901",
                fullmektig = "98765432109",
                leserettigheter = listOf("MED"),
                skriverettigheter = listOf("MED")
            )
        )

        every { mockConsumer.hentKanRepresentere() } returns fullmakter

        val result = service.harSkriverettigheterForMedlemskap("12345678901")

        result shouldBe true
    }

    test("harSkriverettigheterForMedlemskap skal returnere false når kun leserettigheter") {
        val fullmakter = listOf(
            Fullmakt(
                fullmaktsgiver = "12345678901",
                fullmektig = "98765432109",
                leserettigheter = listOf("MED"),
                skriverettigheter = emptyList()
            )
        )

        every { mockConsumer.hentKanRepresentere() } returns fullmakter

        val result = service.harSkriverettigheterForMedlemskap("12345678901")

        result shouldBe false
    }

    test("harSkriverettigheterForMedlemskap skal returnere false når fullmakt ikke finnes") {
        val fullmakter = listOf(
            Fullmakt(
                fullmaktsgiver = "11111111111",
                fullmektig = "98765432109",
                leserettigheter = listOf("MED"),
                skriverettigheter = listOf("MED")
            )
        )

        every { mockConsumer.hentKanRepresentere() } returns fullmakter

        val result = service.harSkriverettigheterForMedlemskap("99999999999")

        result shouldBe false
    }

    test("harSkriverettigheterForMedlemskap skal returnere false ved exception (fail-safe)") {
        every { mockConsumer.hentKanRepresentere() } throws RuntimeException("Nettverksfeil")

        val result = service.harSkriverettigheterForMedlemskap("12345678901")

        result shouldBe false
    }

    test("harLeserettigheterForMedlemskap skal returnere true når fullmakt finnes") {
        val fullmakter = listOf(
            Fullmakt(
                fullmaktsgiver = "12345678901",
                fullmektig = "98765432109",
                leserettigheter = listOf("MED"),
                skriverettigheter = emptyList()
            )
        )

        every { mockConsumer.hentKanRepresentere() } returns fullmakter

        val result = service.harLeserettigheterForMedlemskap("12345678901")

        result shouldBe true
    }

    test("harLeserettigheterForMedlemskap skal returnere true når skriverettigheter finnes") {
        val fullmakter = listOf(
            Fullmakt(
                fullmaktsgiver = "12345678901",
                fullmektig = "98765432109",
                leserettigheter = listOf("MED"),
                skriverettigheter = listOf("MED")
            )
        )

        every { mockConsumer.hentKanRepresentere() } returns fullmakter

        val result = service.harLeserettigheterForMedlemskap("12345678901")

        result shouldBe true
    }

    test("harLeserettigheterForMedlemskap skal returnere false når fullmakt ikke finnes") {
        every { mockConsumer.hentKanRepresentere() } returns emptyList()

        val result = service.harLeserettigheterForMedlemskap("12345678901")

        result shouldBe false
    }

    test("harLeserettigheterForMedlemskap skal returnere false ved exception (fail-safe)") {
        every { mockConsumer.hentKanRepresentere() } throws RuntimeException("Tilgangsfeil")

        val result = service.harLeserettigheterForMedlemskap("12345678901")

        result shouldBe false
    }

    test("skal validere fullmaktsgiver matching korrekt") {
        val fullmakter = listOf(
            // Person A har gitt fullmakt med skriverettigheter
            Fullmakt(
                fullmaktsgiver = "11111111111",
                fullmektig = "98765432109",
                leserettigheter = listOf("MED"),
                skriverettigheter = listOf("MED")
            ),
            // Person B har gitt fullmakt med kun leserettigheter
            Fullmakt(
                fullmaktsgiver = "22222222222",
                fullmektig = "98765432109",
                leserettigheter = listOf("MED"),
                skriverettigheter = emptyList()
            )
        )

        every { mockConsumer.hentKanRepresentere() } returns fullmakter

        // Person A: skal ha skriverettigheter
        service.harSkriverettigheterForMedlemskap("11111111111") shouldBe true

        // Person B: skal IKKE ha skriverettigheter (kun leserettigheter)
        service.harSkriverettigheterForMedlemskap("22222222222") shouldBe false

        // Person B: skal ha leserettigheter
        service.harLeserettigheterForMedlemskap("22222222222") shouldBe true
    }

    test("harSkriverettigheterForMedlemskap skal returnere false når MED ikke i skriverettigheter") {
        val fullmakter = listOf(
            Fullmakt(
                fullmaktsgiver = "12345678901",
                fullmektig = "98765432109",
                leserettigheter = listOf("MED", "DAG"),
                skriverettigheter = listOf("DAG") // MED IKKE i skriverettigheter
            )
        )

        every { mockConsumer.hentKanRepresentere() } returns fullmakter

        val result = service.harSkriverettigheterForMedlemskap("12345678901")

        result shouldBe false
    }

    test("harLeserettigheterForMedlemskap skal returnere false når MED ikke i leserettigheter") {
        val fullmakter = listOf(
            Fullmakt(
                fullmaktsgiver = "12345678901",
                fullmektig = "98765432109",
                leserettigheter = listOf("DAG", "FOS"), // MED IKKE i leserettigheter
                skriverettigheter = listOf("DAG")
            )
        )

        every { mockConsumer.hentKanRepresentere() } returns fullmakter

        val result = service.harLeserettigheterForMedlemskap("12345678901")

        result shouldBe false
    }

    test("skal håndtere flere fullmakter fra samme fullmaktsgiver") {
        val fullmakter = listOf(
            // Samme fullmaktsgiver, forskjellige områder
            Fullmakt(
                fullmaktsgiver = "12345678901",
                fullmektig = "98765432109",
                leserettigheter = listOf("DAG"),
                skriverettigheter = listOf("DAG")
            ),
            Fullmakt(
                fullmaktsgiver = "12345678901",
                fullmektig = "98765432109",
                leserettigheter = listOf("MED"),
                skriverettigheter = listOf("MED")
            )
        )

        every { mockConsumer.hentKanRepresentere() } returns fullmakter

        // Skal finne MED-fullmakten selv om det er flere fullmakter
        service.harSkriverettigheterForMedlemskap("12345678901") shouldBe true
    }
})
