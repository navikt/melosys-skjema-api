package no.nav.melosys.skjema

import io.kotest.matchers.string.shouldNotContain
import java.util.stream.Stream
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.json.JsonMapper

@JsonTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsonIncludeTest {

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @ParameterizedTest(name = "{0}")
    @MethodSource("dtoTestCases")
    @DisplayName("DTOs should exclude null values from JSON serialization")
    fun `DTOs should exclude null values from JSON serialization`(dto: Any) {
        val json = jsonMapper.writeValueAsString(dto)

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