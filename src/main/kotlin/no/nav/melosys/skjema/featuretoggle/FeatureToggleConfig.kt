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

@Configuration
class FeatureToggleConfig {

    /**
     * Synkron førstegangshenting slik at toggles har riktig verdi fra første request etter
     * pod-start – uten den evalueres alt til false frem til første bakgrunns-poll.
     * Manglende UNLEASH_SERVER_API_URL/TOKEN gir bevisst oppstartsfeil (fail-fast).
     */
    @Bean
    @Profile("!local & !local-q1 & !local-q2 & !test")
    fun unleash(
        @Value("\${unleash.token}") token: String,
        @Value("\${unleash.url}") url: String,
        @Value("\${spring.application.name}") appName: String
    ): Unleash = DefaultUnleash(
        UnleashConfig.builder()
            .apiKey(token)
            .appName(appName)
            .unleashAPI(url)
            .synchronousFetchOnInitialisation(true)
            .build()
    ).also { log.info { "Unleash aktivert mot $url" } }

    /** Lokal utvikling og tester kjører uten Unleash-server – alle toggles er på. */
    @Bean
    @Profile("local | local-q1 | local-q2 | test")
    fun fakeUnleash(): Unleash = FakeUnleash()
        .apply { enableAll() }
        .also { log.info { "Lokal/test-profil – bruker FakeUnleash med alle toggles på" } }
}
