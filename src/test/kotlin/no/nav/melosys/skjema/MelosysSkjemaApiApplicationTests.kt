package no.nav.melosys.skjema

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@ActiveProfiles("test")
@SpringBootTest
@ContextConfiguration(initializers = [MockOAuth2ServerInitializer::class])
class MelosysSkjemaApiApplicationTests {

    @Test
    fun contextLoads() {
    }
}