package no.nav.melosys.skjema.validators.felles

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.stream.Stream
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.korrektSyntetiskOrgnr
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrganisasjonsnummerValidatorTest {

    private val eregService = mockk<EregService>()
    private val validator = OrganisasjonsnummerValidator(eregService)

    @BeforeEach
    fun setup() {
        every { eregService.organisasjonsnummerEksisterer(any()) } returns true
    }

    @Test
    fun `should accept valid organization number that exists in registry`() {
        every { eregService.organisasjonsnummerEksisterer(korrektSyntetiskOrgnr) } returns true

        validator.validate(korrektSyntetiskOrgnr) shouldBe emptyList()
    }

    @Test
    fun `should reject valid format but non-existent organization number`() {
        every { eregService.organisasjonsnummerEksisterer(korrektSyntetiskOrgnr) } returns false

        validator.validate(korrektSyntetiskOrgnr) shouldHaveSize 1
    }

    @ParameterizedTest
    @MethodSource("invalidOrganizationNumbers")
    fun `should reject organization numbers with invalid format`(orgnr: String) {
        validator.validate(orgnr) shouldHaveSize 1
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
