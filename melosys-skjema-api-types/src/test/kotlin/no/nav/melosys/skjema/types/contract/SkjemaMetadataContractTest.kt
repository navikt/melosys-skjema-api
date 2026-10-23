package no.nav.melosys.skjema.types.contract

import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.types.utsendtarbeidstaker.AnnenPersonMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.DegSelvMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMedFullmaktMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.RadgiverMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerMetadata
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

class SkjemaMetadataContractTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("goldenFiler")
    fun `metadata overlever round-trip som riktig subtype`(fil: String, forventetType: Class<*>) {
        val metadata = verifiserKontrakt("metadata/$fil", UtsendtArbeidstakerMetadata::class.java)

        metadata.javaClass shouldBe forventetType
    }

    companion object {
        @JvmStatic
        fun goldenFiler() = listOf(
            arguments("deg-selv.json", DegSelvMetadata::class.java),
            arguments("arbeidsgiver.json", ArbeidsgiverMetadata::class.java),
            arguments("arbeidsgiver-med-fullmakt.json", ArbeidsgiverMedFullmaktMetadata::class.java),
            arguments("radgiver.json", RadgiverMetadata::class.java),
            arguments("radgiver-med-fullmakt.json", RadgiverMedFullmaktMetadata::class.java),
            arguments("annen-person.json", AnnenPersonMetadata::class.java)
        )
    }
}
