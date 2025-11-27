package no.nav.melosys.skjema.dto

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.string.shouldNotContain
import java.util.stream.Stream
import no.nav.melosys.skjema.arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier
import no.nav.melosys.skjema.arbeidstakerensLonnDtoMedDefaultVerdier
import no.nav.melosys.skjema.norskeOgUtenlandskeVirksomheterMedDefaultVerdier
import no.nav.melosys.skjema.skatteforholdOgInntektDtoMedDefaultVerdier
import no.nav.melosys.skjema.tilleggsopplysningerDtoMedDefaultVerdier
import no.nav.melosys.skjema.utenlandskVirksomhetMedDefaultVerdier
import no.nav.melosys.skjema.utenlandsoppdragetDtoMedDefaultVerdier
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest

@JsonTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonIncludeTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @ParameterizedTest(name = "{0}")
    @MethodSource("dtoTestCases")
    @DisplayName("DTOs should exclude null values from JSON serialization")
    fun `DTOs should exclude null values from JSON serialization`(dto: Any) {
        val json = objectMapper.writeValueAsString(dto)

        json shouldNotContain "null"
    }

    fun dtoTestCases(): Stream<Arguments> = Stream.of(
        Arguments.of(
            arbeidsgiverensVirksomhetINorgeDtoMedDefaultVerdier().copy(
                opprettholderArbeidsgiverenVanligDrift = null
            )
        ),
        Arguments.of(
            utenlandsoppdragetDtoMedDefaultVerdier().copy(
                ansettelsesforholdBeskrivelse = null
            )
        ),
        Arguments.of(
            arbeidstakerensLonnDtoMedDefaultVerdier().copy(
                virksomheterSomUtbetalerLonnOgNaturalytelser = null
            )
        ),
        Arguments.of(
            skatteforholdOgInntektDtoMedDefaultVerdier().copy(
                landSomUtbetalerPengestotte = null
            )
        ),
        Arguments.of(
            tilleggsopplysningerDtoMedDefaultVerdier().copy(
                tilleggsopplysningerTilSoknad = null
            )
        ),
        Arguments.of(
            norskeOgUtenlandskeVirksomheterMedDefaultVerdier().copy(
                norskeVirksomheter = null
            )
        ),
        Arguments.of(
            utenlandskVirksomhetMedDefaultVerdier().copy(
                bygning = null
            )
        )
    )

}