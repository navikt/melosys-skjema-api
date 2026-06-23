package no.nav.melosys.skjema.integrasjon.felles

import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException

/**
 * Felles hjelpere for RestClient-baserte integrasjoner.
 *
 * RestClient gjør synkrone kall og kaster vanlige exceptions, så vi trenger ikke den
 * reaktive retry-mekanismen fra WebClient – en enkel løkke holder.
 */
object RestClientConfig {

    /**
     * Kjører [call] med inntil [maxAttempts] forsøk totalt. Retryer kun på forbigående feil
     * (HTTP 5xx og forbindelsesfeil) med fast [backoffMillis] ventetid mellom forsøkene.
     * Andre feil kastes umiddelbart.
     */
    fun <T> withRetry(maxAttempts: Int = 3, backoffMillis: Long = 2000, call: () -> T): T {
        repeat(maxAttempts) { attempt ->
            try {
                return call()
            } catch (e: RuntimeException) {
                if (!isTransient(e) || attempt == maxAttempts - 1) throw e
                Thread.sleep(backoffMillis)
            }
        }
        error("withRetry: uoppnåelig tilstand")
    }

    private fun isTransient(throwable: Throwable): Boolean = when (throwable) {
        is HttpServerErrorException -> true
        is ResourceAccessException -> true
        else -> false
    }
}

