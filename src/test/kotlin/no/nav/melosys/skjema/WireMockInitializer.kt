package no.nav.melosys.skjema

import com.github.tomakehurst.wiremock.WireMockServer
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils

class WireMockInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val wireMockServer = WireMockServer(0)
        wireMockServer.start()
        
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            applicationContext,
            "wiremock.server.url=${wireMockServer.baseUrl()}"
        )
        
        applicationContext.getBeanFactory().registerSingleton("wireMockServer", wireMockServer)
    }
}