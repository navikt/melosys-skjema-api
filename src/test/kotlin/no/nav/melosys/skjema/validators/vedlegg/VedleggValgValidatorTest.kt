package no.nav.melosys.skjema.validators.vedlegg

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.types.felles.VedleggValgDto
import no.nav.melosys.skjema.validators.FELT_ER_PAAKREVD
import org.junit.jupiter.api.Test

class VedleggValgValidatorTest {

    private val validator = VedleggValgValidator()

    @Test
    fun `skal være gyldig når harAnnenDokumentasjon er true`() {
        val violations = validator.validate(VedleggValgDto(harAnnenDokumentasjon = true))
        violations.shouldBeEmpty()
    }

    @Test
    fun `skal være gyldig når harAnnenDokumentasjon er false`() {
        val violations = validator.validate(VedleggValgDto(harAnnenDokumentasjon = false))
        violations.shouldBeEmpty()
    }

    @Test
    fun `skal returnere brudd når dto er null`() {
        val violations = validator.validate(null)
        violations.shouldHaveSize(1)
        violations[0].field shouldBe "vedlegg"
        violations[0].translationKey shouldBe FELT_ER_PAAKREVD
    }
}
