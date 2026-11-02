package no.nav.melosys.skjema.validators

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PaakrevdeTekstfelterTest {

    @Test
    fun `gir violation for første felt som mangler eller er blankt`() {
        validerPaakrevdeTekstfelter("a" to "verdi", "b" to " \t", "c" to null) shouldBe listOf(
            Violation(field = "b", translationKey = FELT_ER_PAAKREVD)
        )
    }

    @Test
    fun `gir ingen violations når alle felter har innhold`() {
        validerPaakrevdeTekstfelter("a" to "verdi", "b" to " x ").shouldBeEmpty()
    }
}
