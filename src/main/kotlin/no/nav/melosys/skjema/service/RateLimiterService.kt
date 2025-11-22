package no.nav.melosys.skjema.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.config.RateLimitConfig
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

private val log = KotlinLogging.logger { }

/**
 * Service for rate limiting av API-kall per bruker.
 * Bruker Caffeine cache for å tracke requests per bruker og operasjon.
 */
@Service
class RateLimiterService(
    private val rateLimitConfig: RateLimitConfig
) {

    // Cache key: "${userId}_${operationType}"
    private val requestCache: LoadingCache<String, ConcurrentLinkedQueue<Instant>> = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(5)) // Lengre enn lengste tidsvindu
        .maximumSize(10_000) // Begrens minnebruk
        .build { ConcurrentLinkedQueue() }

    // Dedikerte locks per cache key med automatisk eviction for å unngå minnelekkasje
    private val locks: LoadingCache<String, Any> = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(5))
        .maximumSize(10_000)
        .build { Any() }

    /**
     * Sjekker om brukeren har overskredet rate limit for en gitt operasjon.
     *
     * @param userId Bruker-ID (f.eks. fødselsnummer)
     * @param operationType Type operasjon
     * @return true hvis brukeren har overskredet grensen, false ellers
     */
    fun isRateLimited(
        userId: String,
        operationType: RateLimitOperationType
    ): Boolean {
        val cacheKey = "${userId}_${operationType.name}"
        val lock = locks.get(cacheKey)
        val now = Instant.now()

        val config = rateLimitConfig.getConfigFor(operationType)
        val maxRequests = config.maxRequests
        val timeWindow = config.getTimeWindow()

        // Synkroniser på dedikert lock for å unngå race conditions
        synchronized(lock) {
            val requests = requestCache.get(cacheKey)

            // Fjern gamle requests utenfor tidsvinduet
            val cutoff = now.minus(timeWindow)
            requests.removeIf { it.isBefore(cutoff) }

            // Sjekk om vi er over grensen
            if (requests.size >= maxRequests) {
                log.warn { "Rate limit overskredet for bruker ${userId.take(3)}*** på operasjon '${operationType.name}'" }
                return true
            }

            // Legg til ny request
            requests.add(now)
            return false
        }
    }
}
