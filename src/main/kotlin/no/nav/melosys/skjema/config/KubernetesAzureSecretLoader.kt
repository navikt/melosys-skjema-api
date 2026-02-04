package no.nav.melosys.skjema.config

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

/**
 * Laster Azure client secret fra Kubernetes når applikasjonen kjører med local-q2 profil.
 *
 * Kjører automatisk ved app-start hvis profilen local-q2 er aktiv.
 * Kaller scripts/get-azure-secrets.sh for å hente AZURE_APP_CLIENT_SECRET fra dev-gcp.
 */
class KubernetesAzureSecretLoader : EnvironmentPostProcessor {

    companion object {
        private val LOCAL_Q_PROFILE = arrayOf("local-q2")
        private const val DEFAULT_SCRIPT_PATH = "scripts/get-azure-secrets.sh"
        private val TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        private const val CLASS_NAME = "no.nav.melosys.skjema.config.KubernetesAzureSecretLoader"
        private const val APPLICATION_NAME = "melosys-skjema-api"
    }

    private fun logInfo(message: String) {
        val timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        println("$timestamp |  | $CLASS_NAME | INFO | $message")
    }

    @Suppress("SameParameterValue")
    private fun logWarn(message: String) {
        val timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        println("$timestamp |  | $CLASS_NAME | WARN | $message")
    }

    private fun logError(message: String) {
        val timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        println("$timestamp |  | $CLASS_NAME | ERROR | $message")
    }

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        if (LOCAL_Q_PROFILE.none { profile -> profile in environment.activeProfiles }) {
            return
        }

        val scriptPath = environment.getProperty("kubernetes.azure.script.path", DEFAULT_SCRIPT_PATH)
        val kubeloginPath = environment.getProperty("kubernetes.azure.kubelogin.path")
        val kubectlPath = environment.getProperty("kubernetes.azure.kubectl.path")
        val gcloudPath = environment.getProperty("kubernetes.azure.gcloud.path")

        try {
            logInfo("Laster Azure credentials fra script...")
            val output = executeShellScript(scriptPath, kubeloginPath, kubectlPath, gcloudPath).trim()

            if (output.isNotBlank()) {
                val parts = output.split("|")
                if (parts.size == 2) {
                    val clientId = parts[0]
                    val clientSecret = parts[1]
                    applyAzureCredentials(environment, clientId, clientSecret)
                    logInfo("Lastet inn AZURE_APP_CLIENT_ID og AZURE_APP_CLIENT_SECRET")
                } else {
                    logWarn("Uventet format fra script: $output")
                }
            } else {
                logWarn("Tom output returnert fra script")
            }
        } catch (e: Exception) {
            logError("Feilet med å hente Azure credentials: ${e.message}")
            logError("Sjekk din kubectl og kubelogin config.")
        }
    }

    private fun applyAzureCredentials(environment: ConfigurableEnvironment, clientId: String, clientSecret: String) {
        val properties = mapOf(
            "AZURE_APP_CLIENT_ID" to clientId,
            "AZURE_APP_CLIENT_SECRET" to clientSecret,
            "azure.client.secret" to clientSecret,
            "spring.security.oauth2.client.registration.azure.client-secret" to clientSecret
        )

        val propertySource = MapPropertySource("azure-credentials", properties)
        environment.propertySources.addFirst(propertySource)
        System.setProperty("AZURE_APP_CLIENT_ID", clientId)
        System.setProperty("AZURE_APP_CLIENT_SECRET", clientSecret)
        logInfo("Setter client ID: ${clientId.take(10)}...")
        logInfo("Setter client secret: ${clientSecret.take(3)}...${clientSecret.takeLast(3)}")
    }

    private fun executeShellScript(scriptPath: String, kubeloginPath: String?, kubectlPath: String?, gcloudPath: String?): String {
        val scriptFile = File(System.getProperty("user.dir"), scriptPath)

        if (!scriptFile.exists()) {
            throw RuntimeException("Script ikke funnet: ${scriptFile.absolutePath}")
        }

        if (!scriptFile.canExecute()) {
            scriptFile.setExecutable(true)
        }

        val shell = when {
            System.getProperty("os.name").lowercase().contains("win") -> "cmd.exe"
            else -> System.getenv("SHELL") ?: "/bin/bash"
        }

        val env = HashMap<String, String>(System.getenv())
        val pathSeparator = if (System.getProperty("os.name").lowercase().contains("win")) ";" else ":"
        val pathAdditions = StringBuilder()

        if (!kubeloginPath.isNullOrBlank()) {
            pathAdditions.append(kubeloginPath).append(pathSeparator)
            logInfo("La til kubelogin path til PATH: $kubeloginPath")
        }

        if (!kubectlPath.isNullOrBlank()) {
            pathAdditions.append(kubectlPath).append(pathSeparator)
            logInfo("La til kubectl path til PATH: $kubectlPath")
        }

        if (!gcloudPath.isNullOrBlank()) {
            pathAdditions.append(gcloudPath).append(pathSeparator)
            logInfo("La til gcloud path til PATH: $gcloudPath")
        }

        if (pathAdditions.isNotEmpty()) {
            val currentPath = env["PATH"] ?: ""
            env["PATH"] = pathAdditions.toString() + currentPath
            logInfo("Oppdatert path til PATH: ${env["PATH"]}")
        }

        val command = if (System.getProperty("os.name").lowercase().contains("win")) {
            arrayOf("cmd.exe", "/c", scriptFile.absolutePath, APPLICATION_NAME)
        } else {
            val pathEnvUpdate = if (pathAdditions.isNotEmpty()) {
                "export PATH=\"${pathAdditions}$\${PATH}\";"
            } else ""

            arrayOf(shell, "-c", "$pathEnvUpdate ${scriptFile.absolutePath} $APPLICATION_NAME")
        }

        val processBuilder = ProcessBuilder(*command)
        processBuilder.environment().putAll(env)

        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            line?.let { output.append(it).append("\n") }
        }

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val errorOutput = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                line?.let { errorOutput.append(it).append("\n") }
            }
            throw RuntimeException("Script execution failed with exit code: $exitCode. Error: ${errorOutput.toString().trim()}")
        }

        return output.toString().trim()
    }
}
