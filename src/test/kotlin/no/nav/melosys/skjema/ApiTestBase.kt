package no.nav.melosys.skjema

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ContextConfiguration(
    initializers = [MockOAuth2ServerInitializer::class, WireMockInitializer::class],
    classes = [MelosysSkjemaApiApplication::class]
)
abstract class ApiTestBase