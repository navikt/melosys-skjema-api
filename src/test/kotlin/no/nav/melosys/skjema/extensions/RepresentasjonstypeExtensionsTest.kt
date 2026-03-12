package no.nav.melosys.skjema.extensions

import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import java.util.stream.Stream
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepresentasjonstypeExtensionsTest {

    @ParameterizedTest
    @MethodSource("representasjonstypeTilSkjemadel")
    fun `tilSkjemadel skal returnere riktig skjemadel for representasjonstype`(
        representasjonstype: Representasjonstype,
        forventetSkjemadel: Skjemadel
    ) {
        representasjonstype.tilSkjemadel() shouldBe forventetSkjemadel
    }

    fun representasjonstypeTilSkjemadel(): Stream<Arguments> = Stream.of(
        Arguments.of(Representasjonstype.DEG_SELV, Skjemadel.ARBEIDSTAKERS_DEL),
        Arguments.of(Representasjonstype.ANNEN_PERSON, Skjemadel.ARBEIDSTAKERS_DEL),
        Arguments.of(Representasjonstype.ARBEIDSGIVER, Skjemadel.ARBEIDSGIVERS_DEL),
        Arguments.of(Representasjonstype.RADGIVER, Skjemadel.ARBEIDSGIVERS_DEL),
        Arguments.of(Representasjonstype.ARBEIDSGIVER_MED_FULLMAKT, Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL),
        Arguments.of(Representasjonstype.RADGIVER_MED_FULLMAKT, Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL),
    )
}
