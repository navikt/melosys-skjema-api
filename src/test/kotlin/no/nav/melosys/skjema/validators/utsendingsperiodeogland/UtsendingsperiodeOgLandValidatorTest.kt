package no.nav.melosys.skjema.validators.utsendingsperiodeogland

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.types.felles.LandKode
import no.nav.melosys.skjema.utsendingsperiodeOgLandDtoMedDefaultVerdier
import no.nav.melosys.skjema.validators.FELT_ER_PAAKREVD
import org.junit.jupiter.api.Test

class UtsendingsperiodeOgLandValidatorTest {

    private val validator = UtsendingsperiodeOgLandValidator()

    @Test
    fun `should be valid for non-Norway utsendelsesland`() {
        val dto = utsendingsperiodeOgLandDtoMedDefaultVerdier()
        val violations = validator.validate(dto)
        violations.shouldBeEmpty()
    }

    @Test
    fun `should be invalid when dto is null`() {
        val violations = validator.validate(null)
        violations.shouldHaveSize(1)
        violations.first().translationKey shouldBe FELT_ER_PAAKREVD
    }

    @Test
    fun `should be invalid when utsendelseLand is Norge`() {
        val dto = utsendingsperiodeOgLandDtoMedDefaultVerdier().copy(
            utsendelseLand = LandKode.NO
        )
        val violations = validator.validate(dto)
        violations.shouldHaveSize(1)
        violations.first().field shouldBe "utsendelseLand"
        violations.first().translationKey shouldBe "utsendingsperiodeOgLandTranslation.norgeErIkkeGyldigSomUtsendelsesland"
    }
}
