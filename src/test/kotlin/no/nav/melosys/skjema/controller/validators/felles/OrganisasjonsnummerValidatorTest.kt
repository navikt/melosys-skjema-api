package no.nav.melosys.skjema.controller.validators.felles

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.validation.ConstraintValidatorContext
import java.util.stream.Stream
import no.nav.melosys.skjema.controller.validators.BaseValidatorTest
import no.nav.melosys.skjema.dto.felles.NorskVirksomhet
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrganisasjonsnummerValidatorTest : BaseValidatorTest() {

    private val organisasjonsnummerValidator by lazy { OrganisasjonsnummerValidator(eregService) }
    private val context = mockk<ConstraintValidatorContext>(relaxed = true)

    @ParameterizedTest
    @MethodSource("fieldsWithOrganisasjonsnummer")
    fun `fields with organisasjonsnummer should be annotated with ErOrganisasjonsnummer`(clazz: Class<*>, fieldName: String) {
        val field = clazz.getDeclaredField(fieldName)
        val annotation = field.getAnnotation(ErOrganisasjonsnummer::class.java)
        annotation.shouldNotBeNull()
    }

    fun fieldsWithOrganisasjonsnummer(): Stream<Arguments> = listOf(
        Arguments.of(NorskVirksomhet::class.java, "organisasjonsnummer")
    ).stream()

    @Test
    fun `should accept valid organization number that exists in registry`() {
        every { eregService.organisasjonsnummerEksisterer(korrektSyntetiskOrgnr) } returns true

        val isValid = organisasjonsnummerValidator.isValid(korrektSyntetiskOrgnr, context)

        isValid shouldBe true
    }

    @Test
    fun `should reject valid format but non-existent organization number`() {
        every { eregService.organisasjonsnummerEksisterer(korrektSyntetiskOrgnr) } returns false

        val isValid = organisasjonsnummerValidator.isValid(korrektSyntetiskOrgnr, context)

        isValid shouldBe false
    }

    @ParameterizedTest
    @MethodSource("invalidOrganizationNumbers")
    fun `should reject organization numbers with invalid format`(orgnr: String) {
        val isValid = organisasjonsnummerValidator.isValid(orgnr, context)

        isValid shouldBe false
    }

    @Test
    fun `organisasjonsnummerHarFormat should accept valid organization number`() {
        OrganisasjonsnummerValidator.organisasjonsnummerHarGyldigFormat(korrektSyntetiskOrgnr) shouldBe true
    }

    @ParameterizedTest
    @MethodSource("invalidOrganizationNumbers")
    fun `organisasjonsnummerHarFormat should reject invalid formats`(orgnr: String) {
        OrganisasjonsnummerValidator.organisasjonsnummerHarGyldigFormat(orgnr) shouldBe false
    }

    fun invalidOrganizationNumbers(): Stream<Arguments> = listOf(
        "12345678",      // Too short
        "1234567890",    // Too long
        "12345678A",     // Contains letters
        "ABC123456",     // Contains letters
        "312587964"      // Wrong MOD11 checksum
    ).map { Arguments.of(it) }.stream()
}
