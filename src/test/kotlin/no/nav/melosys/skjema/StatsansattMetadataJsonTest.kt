package no.nav.melosys.skjema

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.melosys.skjema.types.SkjemaMetadata
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.kotlinModule

class StatsansattMetadataJsonTest {

    private val mapper = JsonMapper.builder().addModule(kotlinModule()).build()

    @ParameterizedTest
    @EnumSource(Representasjonstype::class)
    fun `eldre metadata leses med nye fagbegreper for alle representasjonstyper`(representasjonstype: Representasjonstype) {
        for (verdi in listOf(true, false, null)) {
            val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
                representasjonstype = representasjonstype,
                erStatligArbeidsgiver = verdi,
                erStatsansatt = verdi,
                harArbeidsforholdIAareg = verdi,
            )
            val json = mapper.writeValueAsString(metadata)
            val eldreJson = json
                .replace("\"erStatligArbeidsgiver\"", "\"erOffentligArbeidsgiver\"")
                .replace("\"erStatsansatt\"", "\"erOffentligTjenesteperson\"")

            mapper.readValue(eldreJson, SkjemaMetadata::class.java) shouldBe metadata
            mapper.readValue(json, SkjemaMetadata::class.java) shouldBe metadata
            json shouldContain "\"erStatligArbeidsgiver\""
            json shouldContain "\"erStatsansatt\""
            json shouldNotContain "\"erOffentligArbeidsgiver\""
            json shouldNotContain "\"erOffentligTjenesteperson\""
        }
    }

    @ParameterizedTest
    @EnumSource(Representasjonstype::class)
    fun `metadata uten eldre klassifiseringsfelt beholder null`(representasjonstype: Representasjonstype) {
        val metadata = utsendtArbeidstakerMetadataMedDefaultVerdier(
            representasjonstype = representasjonstype,
            erStatligArbeidsgiver = null,
            erStatsansatt = null,
        )
        val json = mapper.valueToTree<ObjectNode>(metadata)
        json.remove("erStatligArbeidsgiver")
        json.remove("erStatsansatt")

        mapper.treeToValue(json, SkjemaMetadata::class.java) shouldBe metadata
    }
}
