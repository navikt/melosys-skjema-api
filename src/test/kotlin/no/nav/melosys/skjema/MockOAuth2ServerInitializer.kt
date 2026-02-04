package no.nav.melosys.skjema

import java.util.function.Supplier
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.support.GenericApplicationContext

class MockOAuth2ServerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    private val mockOauthServerBaseUrl = "mock-oauth2-server.baseUrl"

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val server = registerMockOAuth2Server(applicationContext)
        val baseUrl = server.baseUrl().toString().replace("/$".toRegex(), "")

        TestPropertyValues
            .of(mapOf(mockOauthServerBaseUrl to baseUrl))
            .applyTo(applicationContext)
    }

    private fun registerMockOAuth2Server(applicationContext: ConfigurableApplicationContext): MockOAuth2Server {
        val server = MockOAuth2Server()
        server.start()
        (applicationContext as GenericApplicationContext).registerBean(
            MockOAuth2Server::class.java,
            Supplier { server },
        )

        return server
    }
}
