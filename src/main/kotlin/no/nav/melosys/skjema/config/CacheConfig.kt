package no.nav.melosys.skjema.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(
        @Value("\${repr.cache.ttl-minutes:5}") reprCacheTtlMinutes: Long
    ): CacheManager {
        val cacheManager = CaffeineCacheManager()

        // Registrer alle cache-navn
        cacheManager.setCacheNames(listOf("fullmakter"))

        // Konfigurasjon per cache
        cacheManager.registerCustomCache(
            "fullmakter",
            Caffeine.newBuilder()
                .expireAfterWrite(reprCacheTtlMinutes, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build()
        )

        return cacheManager
    }
}
