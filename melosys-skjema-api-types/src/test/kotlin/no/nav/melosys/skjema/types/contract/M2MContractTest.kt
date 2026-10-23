package no.nav.melosys.skjema.types.contract

import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.junit.jupiter.api.Test

class M2MContractTest {

    @Test
    fun `M2M-skjema overlever round-trip`() {
        verifiserKontrakt("m2m/utsendt-arbeidstaker-m2m.json", UtsendtArbeidstakerSkjemaM2MDto::class.java)
    }

    @Test
    fun `Kafka-melding om mottatt skjema overlever round-trip`() {
        verifiserKontrakt("kafka/skjema-mottatt-melding.json", SkjemaMottattMelding::class.java)
    }
}
