package no.nav.melosys.skjema.integrasjon.ereg.dto

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ActiveProfiles

@JsonTest
@ActiveProfiles("test")
class OrganisasjonTest {

    @Autowired
    private lateinit var json: JacksonTester<Organisasjon>

    @Test
    fun `skal deserialisere organisasjon json`() {
        val resource = ClassPathResource("ereg/organisasjonResponse.json")
        val organisasjon = json.read(resource).`object`

        organisasjon.shouldBeInstanceOf<Virksomhet>()
        organisasjon.organisasjonsnummer shouldBe "990983666"

        val organisasjonsledd = organisasjon.bestaarAvOrganisasjonsledd?.firstOrNull()?.organisasjonsledd
        organisasjonsledd.shouldNotBeNull()
        organisasjonsledd.shouldBeInstanceOf<Organisasjonsledd>()
        organisasjonsledd.organisasjonsnummer shouldBe "990983291"

        val organisasjonsleddOver = organisasjonsledd.organisasjonsleddOver?.firstOrNull()?.organisasjonsledd
        organisasjonsleddOver.shouldNotBeNull()
        organisasjonsleddOver.shouldBeInstanceOf<Organisasjonsledd>()
        organisasjonsleddOver.organisasjonsnummer shouldBe "889640782"

        val inngaarIJuridiskEnhet = organisasjonsleddOver.inngaarIJuridiskEnheter?.firstOrNull()
        inngaarIJuridiskEnhet.shouldNotBeNull()
        inngaarIJuridiskEnhet.shouldBeInstanceOf<InngaarIJuridiskEnhet>()
        inngaarIJuridiskEnhet.organisasjonsnummer shouldBe "983887457"
    }
}