package no.nav.melosys.skjema.featuretoggle

import io.getunleash.DefaultUnleash
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

private val log = KotlinLogging.logger { }

/**
 * De to profil-uttrykkene under MÅ holdes som eksakte komplementer: en ny lokal profil som
 * legges til bare ett sted gir enten to Unleash-bønner eller oppstartsfeil pga. manglende
 * UNLEASH_SERVER_API_URL/TOKEN (sistnevnte er tilsiktet fail-fast i deployede miljøer).
 */
private const val LOKALE_PROFILER = "local | local-q1 | local-q2 | test"
private const val DEPLOYEDE_PROFILER = "!local & !local-q1 & !local-q2 & !test"

@Configuration
class FeatureToggleConfig {

    /**
     * Bevisst ASYNKRON førstegangshenting (SDK-en henter umiddelbart ved oppstart, og
     * readiness-proben med 20 s initialDelay dekker det korte vinduet der toggles ellers
     * ville evaluert til false). Synkron henting ville gjort forbigående Unleash-nedetid
     * til CrashLoopBackOff for hele appen.
     */
    @Bean
    @Profile(DEPLOYEDE_PROFILER)
    fun unleash(
        @Value("\${unleash.token}") token: String,
        @Value("\${unleash.url}") url: String,
        @Value("\${spring.application.name}") appName: String
    ): Unleash = DefaultUnleash(
        UnleashConfig.builder()
            .apiKey(token)
            .appName(appName)
            .unleashAPI(url)
            .build()
    ).also { log.info { "Unleash aktivert mot $url" } }

    /** Lokal utvikling og tester kjører uten Unleash-server – alle toggles er på. */
    @Bean
    @Profile(LOKALE_PROFILER)
    fun fakeUnleash(): Unleash = FakeUnleash()
        .apply { enableAll() }
        .also { log.info { "Lokal/test-profil – bruker FakeUnleash med alle toggles på" } }
}
