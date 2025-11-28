package no.nav.melosys.skjema.controller.validators

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import jakarta.validation.ConstraintValidatorContext
import java.util.stream.Stream
import no.nav.melosys.skjema.controller.dto.VerifiserPersonRequest
import no.nav.melosys.skjema.korrektSyntetiskFnr
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ErFodselsEllerDNummerValidatorTest : BaseValidatorTest() {

    private val fodselsEllerDNummerValidator = ErFodselsEllerDNummerValidator(validerSyntetiskFnr = true)
    private val context = mockk<ConstraintValidatorContext>(relaxed = true)

    @ParameterizedTest
    @MethodSource("fieldsWithFodselsnummer")
    fun `fields with fodselsnummer should be annotated with ErFodselsEllerDNummer`(clazz: Class<*>, fieldName: String) {
        val field = clazz.getDeclaredField(fieldName)
        val annotation = field.getAnnotation(ErFodselsEllerDNummer::class.java)
        annotation.shouldNotBeNull()
    }

    fun fieldsWithFodselsnummer(): Stream<Arguments> = listOf(
        Arguments.of(VerifiserPersonRequest::class.java, "fodselsnummer")
    ).stream()

    @Test
    fun `should accept valid synthetic fodselsnummer`() {
        val isValid = fodselsEllerDNummerValidator.isValid(korrektSyntetiskFnr, context)

        isValid shouldBe true
    }

    @ParameterizedTest
    @MethodSource("invalidFodselsnummer")
    fun `should reject fodselsnummer with invalid format`(fnr: String) {
        val isValid = fodselsEllerDNummerValidator.isValid(fnr, context)

        isValid shouldBe false
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
