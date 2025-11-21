package no.nav.melosys.skjema.integrasjon.repr.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest

@JsonTest
class FullmaktTest {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `Fullmakt skal serialisere og deserialisere korrekt`() {
        val fullmakt = Fullmakt(
            fullmaktsgiver = "12345678901",
            fullmektig = "98765432109",
            leserettigheter = listOf("MED", "DAG"),
            skriverettigheter = listOf("MED")
        )

        val json = objectMapper.writeValueAsString(fullmakt)
        val deserialized = objectMapper.readValue<Fullmakt>(json)

        deserialized shouldBe fullmakt
    }

    @Test
    fun `Fullmakt skal ignorere ukjente felter ved deserialisering`() {
        val jsonWithUnknownFields = """
            {
              "fullmaktsgiver": "12345678901",
              "fullmektig": "98765432109",
              "leserettigheter": ["MED", "DAG"],
              "skriverettigheter": ["MED"],
              "unknownField1": "should be ignored",
              "unknownField2": {
                "nested": "also ignored"
              }
            }
        """.trimIndent()

        val fullmakt = objectMapper.readValue<Fullmakt>(jsonWithUnknownFields)

        fullmakt.fullmaktsgiver shouldBe "12345678901"
        fullmakt.fullmektig shouldBe "98765432109"
        fullmakt.leserettigheter shouldBe listOf("MED", "DAG")
        fullmakt.skriverettigheter shouldBe listOf("MED")
    }

    @Test
    fun `Fullmakt skal håndtere tomme lister for rettigheter`() {
        val json = """
            {
              "fullmaktsgiver": "12345678901",
              "fullmektig": "98765432109",
              "leserettigheter": [],
              "skriverettigheter": []
            }
        """.trimIndent()

        val fullmakt = objectMapper.readValue<Fullmakt>(json)

        fullmakt.leserettigheter shouldBe emptyList()
        fullmakt.skriverettigheter shouldBe emptyList()
    }
}
