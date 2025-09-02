package no.nav.melosys.skjema.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcAsyncConfig : WebMvcConfigurer {

    override fun configureAsyncSupport(configurer: AsyncSupportConfigurer) {
        // Set timeout for async requests (60 seconds)
        configurer.setDefaultTimeout(60000)
        
        // Register async interceptors if needed
        // This ensures request context is available in async threads
        configurer.registerCallableInterceptors()
        configurer.registerDeferredResultInterceptors()
    }
}