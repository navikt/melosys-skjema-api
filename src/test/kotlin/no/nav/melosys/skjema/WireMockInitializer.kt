package no.nav.melosys.skjema

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils

class WireMockInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        // HTTP/1.1 mot mocken – unngår flaky HTTP/2 (h2c) i test.
        val wireMockServer = WireMockServer(
            WireMockConfiguration.options()
                .dynamicPort()
                .http2PlainDisabled(true)
        )
        wireMockServer.start()
        
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            applicationContext,
            "wiremock.server.url=${wireMockServer.baseUrl()}"
        )
        
        applicationContext.getBeanFactory().registerSingleton("wireMockServer", wireMockServer)
    }
}