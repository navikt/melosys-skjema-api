package no.nav.melosys.skjema.config

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(
        @Value("\${repr.cache.ttl-minutes:5}") reprCacheTtlMinutes: Long
    ): CacheManager {
        val cacheManager = CaffeineCacheManager()

        // Registrer alle cache-navn
        cacheManager.setCacheNames(listOf("fullmakter", "ereg", "pdl-person", "pdl-personer-bulk"))

        // Konfigurasjon per cache
        // fullmakter: hvem innlogget bruker kan representere (hvor bruker er fullmektig)
        cacheManager.registerCustomCache(
            "fullmakter",
            Caffeine.newBuilder()
                .expireAfterWrite(reprCacheTtlMinutes, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build()
        )

        // ereg: organisasjonsinformasjon fra EREG
        cacheManager.registerCustomCache(
            "ereg",
            Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build()
        )

        // pdl-person: person-verifisering fra PDL
        cacheManager.registerCustomCache(
            "pdl-person",
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build()
        )

        // pdl-personer-bulk: bulk-henting av personer fra PDL
        cacheManager.registerCustomCache(
            "pdl-personer-bulk",
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .build()
        )

        return cacheManager
    }
}
