package no.nav.melosys.skjema.validators.felles

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import java.util.stream.Stream
import no.nav.melosys.skjema.korrektSyntetiskFnr
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ErFodselsEllerDNummerValidatorTest {

    private val validator = ErFodselsEllerDNummerValidator(validerSyntetiskFnr = true)

    @Test
    fun `should accept valid synthetic fodselsnummer`() {
        validator.validate(korrektSyntetiskFnr).shouldBeEmpty()
    }

    @ParameterizedTest
    @MethodSource("invalidFodselsnummer")
    fun `should reject fodselsnummer with invalid format`(fnr: String) {
        validator.validate(fnr).shouldHaveSize(1)
    }

    fun invalidFodselsnummer(): Stream<Arguments> = listOf(
        "1234567890",     // Too short (10 digits)
        "123456789012",   // Too long (12 digits)
        "1234567890A",    // Contains letters
        "02837999891",    // Wrong checksum
        "",               // Empty string
        "12345678901"     // Invalid date
    ).map { Arguments.of(it) }.stream()
}
