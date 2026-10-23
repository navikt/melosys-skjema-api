package no.nav.melosys.skjema.types.contract

import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaData
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

class SkjemaDataContractTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("goldenFiler")
    fun `skjemadata overlever round-trip som riktig subtype`(fil: String, forventetType: Class<*>) {
        val data = verifiserKontrakt("skjemadata/$fil", UtsendtArbeidstakerSkjemaData::class.java)

        data.javaClass shouldBe forventetType
    }

    companion object {
        @JvmStatic
        fun goldenFiler() = listOf(
            arguments("arbeidsgiver-data.json", UtsendtArbeidstakerArbeidsgiversSkjemaDataDto::class.java),
            arguments("arbeidstaker-data.json", UtsendtArbeidstakerArbeidstakersSkjemaDataDto::class.java),
            arguments("kombinert-data.json", UtsendtArbeidstakerArbeidsgiverOgArbeidstakerSkjemaDataDto::class.java)
        )
    }
}
