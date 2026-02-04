package no.nav.melosys.skjema.scheduler

import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import net.javacrumbs.shedlock.core.LockAssert
import no.nav.melosys.skjema.config.InnsendingRetryConfig
import java.util.*
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.service.InnsendingService
import no.nav.melosys.skjema.skjemaMedDefaultVerdier

class InnsendingRetrySchedulerTest : FunSpec({

    beforeSpec {
        LockAssert.TestHelper.makeAllAssertsPass(true)
    }

    afterSpec {
        LockAssert.TestHelper.makeAllAssertsPass(false)
    }

    val innsendingService = mockk<InnsendingService>()

    val retryConfig = InnsendingRetryConfig().apply {
        fixedDelayMinutes = 5
        initialDelaySeconds = 60
        maxAttempts = 5
        staleThresholdMinutes = 5
    }

    val scheduler = InnsendingRetryScheduler(innsendingService, retryConfig)

    afterTest {
        clearAllMocks()
    }

    fun retryKandidat() = innsendingMedDefaultVerdier(
        id = UUID.randomUUID(),
        skjema = skjemaMedDefaultVerdier(id = UUID.randomUUID()),
        status = InnsendingStatus.KAFKA_FEILET
    )

    test("Skal kjøre retry på kandidater fra innsendingService") {

        val kandidat1 = retryKandidat()
        val kandidat2 = retryKandidat()

        every { innsendingService.hentRetryKandidater(
            any(), eq(retryConfig.maxAttempts))
        } returns listOf(kandidat1, kandidat2)
        every { innsendingService.prosesserInnsending(any()) } returns Unit

        scheduler.retryFeiledeInnsendinger()

        verify { innsendingService.prosesserInnsending(kandidat1.skjema.id!!) }
        verify { innsendingService.prosesserInnsending(kandidat2.skjema.id!!) }
    }

    test("Skal håndtere feil ved prosessering av innsending uten å stoppe hele jobben") {

        val kandidat1 = retryKandidat()
        val kandidat2 = retryKandidat()

        every { innsendingService.hentRetryKandidater(
            any(), eq(retryConfig.maxAttempts))
        } returns listOf(kandidat1, kandidat2)
        every { innsendingService.prosesserInnsending(kandidat1.skjema.id!!) } throws RuntimeException("Test feil")
        every { innsendingService.prosesserInnsending(kandidat2.skjema.id!!) } returns Unit

        scheduler.retryFeiledeInnsendinger()

        verify { innsendingService.prosesserInnsending(kandidat1.skjema.id!!) }
        verify { innsendingService.prosesserInnsending(kandidat2.skjema.id!!) }
    }


})

