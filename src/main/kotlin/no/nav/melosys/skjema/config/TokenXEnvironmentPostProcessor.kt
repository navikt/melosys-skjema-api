package no.nav.melosys.skjema.config

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class TokenXEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val activeProfiles = environment.activeProfiles
        val validProfiles = setOf("local-q1", "local-q2")
        
        if (!activeProfiles.any { it in validProfiles }) {
            return
        }
        
        val process = ProcessBuilder("scripts/get-tokenx-secrets.sh")
            .directory(java.io.File(System.getProperty("user.dir")))
            .start()
        
        val secretsScriptOutput = process.inputStream.bufferedReader().use { it.readText().trim() }
        val exitCode = process.waitFor()
        
        if (exitCode != 0) {
            throw RuntimeException("Failed to execute TokenX secrets script. Exit code: $exitCode")
        }
        
        if (secretsScriptOutput.isBlank()) {
            throw RuntimeException("TokenX secrets script returned empty output")
        }
        
        val properties = mapOf<String, Any>(
            "TOKEN_X_PRIVATE_JWK" to secretsScriptOutput
        )
        
        val propertySource = MapPropertySource("tokenx-private-jwk", properties)
        environment.propertySources.addFirst(propertySource)
    }
}