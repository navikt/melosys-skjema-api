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

        val privateJwkValue = getScriptOutput("scripts/get-tokenx-private-jwk.sh")
        val gcloudAccountId = getScriptOutput("scripts/get-gcloud-account.sh")

        val properties = mapOf<String, Any>(
            "TOKEN_X_PRIVATE_JWK" to privateJwkValue,
            "DB_JDBC_URL" to "jdbc:postgresql://localhost:5432/melosys-skjema?user=$gcloudAccountId",
        )
        
        val propertySource = MapPropertySource("tokenx-private-jwk", properties)
        environment.propertySources.addFirst(propertySource)
    }

    private fun getScriptOutput(scriptPath: String): String {
        val process = ProcessBuilder(scriptPath)
            .directory(java.io.File(System.getProperty("user.dir")))
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText().trim() }
        val errorOutput = process.errorStream.bufferedReader().use { it.readText().trim() }
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw RuntimeException("Failed to execute script at $scriptPath. Exit code: $exitCode. Error: $errorOutput")
        }

        return output
    }
}