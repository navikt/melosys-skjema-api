package no.nav.melosys.skjema

import no.nav.melosys.skjema.config.TestConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(
    initializers = [MockOAuth2ServerInitializer::class],
    classes = [MelosysSkjemaApiApplication::class, TestConfiguration::class]
)
abstract class ApiTestBase