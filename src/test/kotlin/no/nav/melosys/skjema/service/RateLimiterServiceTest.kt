package no.nav.melosys.skjema.service

import io.mockk.every
import io.mockk.mockk
import java.time.Duration
import no.nav.melosys.skjema.config.RateLimitConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RateLimiterServiceTest {

    private val typeConfig = RateLimitConfig.TypeConfig(
        maxRequests = 10,
        timeWindowMinutes = 1
    )
    private val rateLimitConfig = mockk<RateLimitConfig>().apply {
        every { getConfigFor(any()) } returns typeConfig
    }
    private val rateLimiterService = RateLimiterService(rateLimitConfig)

    @Test
    fun `isRateLimited returnerer false når ingen requests er gjort`() {
        val result = rateLimiterService.isRateLimited("user1", RateLimitOperationType.ORGANISASJONSSOK)

        assertThat(result).isFalse()
    }

    @Test
    fun `isRateLimited returnerer false når antall requests er under grensen`() {
        val userId = "user2"

        // Gjør 5 requests (under grensen på 10)
        repeat(5) {
            rateLimiterService.isRateLimited(userId, RateLimitOperationType.ORGANISASJONSSOK)
        }

        val result = rateLimiterService.isRateLimited(userId, RateLimitOperationType.ORGANISASJONSSOK)

        assertThat(result).isFalse()
    }

    @Test
    fun `isRateLimited returnerer true når grensen er nådd`() {
        val userId = "user3"

        // Gjør 10 requests (grensen)
        repeat(10) {
            rateLimiterService.isRateLimited(userId, RateLimitOperationType.ORGANISASJONSSOK)
        }

        // 11. request skal blokkeres
        val result = rateLimiterService.isRateLimited(userId, RateLimitOperationType.ORGANISASJONSSOK)

        assertThat(result).isTrue()
    }

    @Test
    fun `ulike brukere har separate rate limits`() {
        val user1 = "user5"
        val user2 = "user6"

        // Gjør 10 requests for user1 (grensen)
        repeat(10) {
            rateLimiterService.isRateLimited(user1, RateLimitOperationType.ORGANISASJONSSOK)
        }

        // user1 skal være rate limited
        assertThat(rateLimiterService.isRateLimited(user1, RateLimitOperationType.ORGANISASJONSSOK)).isTrue()

        // user2 skal fortsatt kunne gjøre requests
        assertThat(rateLimiterService.isRateLimited(user2, RateLimitOperationType.ORGANISASJONSSOK)).isFalse()
    }

    @Test
    fun `gamle requests fjernes fra tellingen`() {
        val shortTypeConfig = RateLimitConfig.TypeConfig(
            maxRequests = 10,
            timeWindowMinutes = 0 // Sett til 0 for å overstyre i test
        ).apply {
            // Overstyr getTimeWindow() i testen ved å sette duration direkte
        }
        val shortTimeWindowConfig = mockk<RateLimitConfig>().apply {
            every { getConfigFor(any()) } returns mockk<RateLimitConfig.TypeConfig>().apply {
                every { maxRequests } returns 10
                every { getTimeWindow() } returns Duration.ofMillis(100)
            }
        }
        val serviceWithShortWindow = RateLimiterService(shortTimeWindowConfig)
        val userId = "user7"

        // Gjør 10 requests med kort tidsvindu (100ms)
        repeat(10) {
            serviceWithShortWindow.isRateLimited(userId, RateLimitOperationType.ORGANISASJONSSOK)
        }

        // user skal være rate limited
        assertThat(serviceWithShortWindow.isRateLimited(userId, RateLimitOperationType.ORGANISASJONSSOK)).isTrue()

        // Vent til tidsvinduet har passert
        Thread.sleep(150)

        // Nå skal rate limit være resatt
        assertThat(serviceWithShortWindow.isRateLimited(userId, RateLimitOperationType.ORGANISASJONSSOK)).isFalse()
    }
}
