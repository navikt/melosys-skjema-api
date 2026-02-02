package no.nav.melosys.skjema.repository

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.melosys.skjema.ApiTestBase
import no.nav.melosys.skjema.domain.InnsendingStatus
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.innsendingMedDefaultVerdier
import no.nav.melosys.skjema.skjemaMedDefaultVerdier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Integrasjonstester for InnsendingRepository.
 *
 * Tester JPQL-queryen findRetryKandidater som har kompleks logikk
 * med OR-betingelser og IN-klausul for statusfiltrering.
 */
class InnsendingRepositoryIntegrationTest : ApiTestBase() {

    @Autowired
    private lateinit var innsendingRepository: InnsendingRepository

    @Autowired
    private lateinit var skjemaRepository: SkjemaRepository

    @BeforeEach
    fun setUp() {
        innsendingRepository.deleteAll()
        skjemaRepository.deleteAll()
    }

    @Nested
    @DisplayName("findRetryKandidater")
    inner class FindRetryKandidaterTests {

        @Test
        @DisplayName("Skal returnere tom liste når ingen innsendinger finnes")
        fun `skal returnere tom liste når ingen innsendinger finnes`() {
            val grense = Instant.now().minus(5, ChronoUnit.MINUTES)

            val kandidater = innsendingRepository.findRetryKandidater(grense, maxAttempts = 5)

            kandidater.shouldBeEmpty()
        }

        @Test
        @DisplayName("Skal finne MOTTATT innsending eldre enn grense")
        fun `skal finne MOTTATT innsending eldre enn grense`() {
            val gammelOpprettetDato = Instant.now().minus(10, ChronoUnit.MINUTES)
            val innsending = innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = opprettSkjema(),
                    status = InnsendingStatus.MOTTATT,
                    opprettetDato = gammelOpprettetDato
                )
            )

            val grense = Instant.now().minus(5, ChronoUnit.MINUTES)
            val kandidater = innsendingRepository.findRetryKandidater(grense, maxAttempts = 5)

            kandidater shouldHaveSize 1
            kandidater[0].id shouldBe innsending.id
        }

        @Test
        @DisplayName("Skal IKKE finne MOTTATT innsending nyere enn grense")
        fun `skal ikke finne MOTTATT innsending nyere enn grense`() {
            val nyOpprettetDato = Instant.now().minus(2, ChronoUnit.MINUTES)
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = opprettSkjema(),
                    status = InnsendingStatus.MOTTATT,
                    opprettetDato = nyOpprettetDato
                )
            )

            val grense = Instant.now().minus(5, ChronoUnit.MINUTES)
            val kandidater = innsendingRepository.findRetryKandidater(grense, maxAttempts = 5)

            kandidater.shouldBeEmpty()
        }

        @Test
        @DisplayName("Skal finne JOURNALFORING_FEILET med færre enn maxAttempts")
        fun `skal finne JOURNALFORING_FEILET med færre enn maxAttempts`() {
            val innsending = innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = opprettSkjema(),
                    status = InnsendingStatus.JOURNALFORING_FEILET,
                    antallForsok = 2
                )
            )

            val grense = Instant.now().minus(5, ChronoUnit.MINUTES)
            val kandidater = innsendingRepository.findRetryKandidater(grense, maxAttempts = 5)

            kandidater shouldHaveSize 1
            kandidater[0].id shouldBe innsending.id
        }

        @Test
        @DisplayName("Skal finne KAFKA_FEILET med færre enn maxAttempts")
        fun `skal finne KAFKA_FEILET med færre enn maxAttempts`() {
            val innsending = innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = opprettSkjema(),
                    status = InnsendingStatus.KAFKA_FEILET,
                    antallForsok = 3
                )
            )

            val grense = Instant.now().minus(5, ChronoUnit.MINUTES)
            val kandidater = innsendingRepository.findRetryKandidater(grense, maxAttempts = 5)

            kandidater shouldHaveSize 1
            kandidater[0].id shouldBe innsending.id
        }

        @Test
        @DisplayName("Skal IKKE finne feilet innsending med maxAttempts nådd")
        fun `skal ikke finne feilet innsending med maxAttempts nådd`() {
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = opprettSkjema(),
                    status = InnsendingStatus.JOURNALFORING_FEILET,
                    antallForsok = 5
                )
            )

            val grense = Instant.now().minus(5, ChronoUnit.MINUTES)
            val kandidater = innsendingRepository.findRetryKandidater(grense, maxAttempts = 5)

            kandidater.shouldBeEmpty()
        }

        @Test
        @DisplayName("Skal IKKE finne FERDIG innsending")
        fun `skal ikke finne FERDIG innsending`() {
            innsendingRepository.save(innsendingMedDefaultVerdier(skjema = opprettSkjema(), status = InnsendingStatus.FERDIG))

            val grense = Instant.now().minus(5, ChronoUnit.MINUTES)
            val kandidater = innsendingRepository.findRetryKandidater(grense, maxAttempts = 5)

            kandidater.shouldBeEmpty()
        }

        @Test
        @DisplayName("Skal IKKE finne JOURNALFORT innsending")
        fun `skal ikke finne JOURNALFORT innsending`() {
            innsendingRepository.save(innsendingMedDefaultVerdier(skjema = opprettSkjema(), status = InnsendingStatus.JOURNALFORT))

            val grense = Instant.now().minus(5, ChronoUnit.MINUTES)
            val kandidater = innsendingRepository.findRetryKandidater(grense, maxAttempts = 5)

            kandidater.shouldBeEmpty()
        }

        @Test
        @DisplayName("Skal finne kombinasjon av MOTTATT og feilede innsendinger")
        fun `skal finne kombinasjon av MOTTATT og feilede innsendinger`() {
            val gammelOpprettetDato = Instant.now().minus(10, ChronoUnit.MINUTES)

            val mottattInnsending = innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = opprettSkjema(),
                    status = InnsendingStatus.MOTTATT,
                    opprettetDato = gammelOpprettetDato
                )
            )
            val journalforingFeiletInnsending = innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = opprettSkjema(),
                    status = InnsendingStatus.JOURNALFORING_FEILET,
                    antallForsok = 1
                )
            )
            val kafkaFeiletInnsending = innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = opprettSkjema(),
                    status = InnsendingStatus.KAFKA_FEILET,
                    antallForsok = 2
                )
            )
            // Disse skal IKKE inkluderes:
            innsendingRepository.save(innsendingMedDefaultVerdier(skjema = opprettSkjema(), status = InnsendingStatus.FERDIG))
            innsendingRepository.save(innsendingMedDefaultVerdier(skjema = opprettSkjema(), status = InnsendingStatus.JOURNALFORT))
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = opprettSkjema(),
                    status = InnsendingStatus.MOTTATT,
                    opprettetDato = Instant.now() // For ny
                )
            )
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = opprettSkjema(),
                    status = InnsendingStatus.JOURNALFORING_FEILET,
                    antallForsok = 5 // Max nådd
                )
            )

            val grense = Instant.now().minus(5, ChronoUnit.MINUTES)
            val kandidater = innsendingRepository.findRetryKandidater(grense, maxAttempts = 5)

            kandidater shouldHaveSize 3
            kandidater.map { it.id } shouldContainExactlyInAnyOrder listOf(
                mottattInnsending.id,
                journalforingFeiletInnsending.id,
                kafkaFeiletInnsending.id
            )
        }

        @Test
        @DisplayName("Skal respektere maxAttempts parameter")
        fun `skal respektere maxAttempts parameter`() {
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = opprettSkjema(),
                    status = InnsendingStatus.JOURNALFORING_FEILET,
                    antallForsok = 2
                )
            )
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = opprettSkjema(),
                    status = InnsendingStatus.KAFKA_FEILET,
                    antallForsok = 3
                )
            )

            val grense = Instant.now().minus(5, ChronoUnit.MINUTES)

            // Med maxAttempts=3 skal kun den med antallForsok=2 inkluderes
            val kandidaterMed3 = innsendingRepository.findRetryKandidater(grense, maxAttempts = 3)
            kandidaterMed3 shouldHaveSize 1

            // Med maxAttempts=4 skal begge inkluderes
            val kandidaterMed4 = innsendingRepository.findRetryKandidater(grense, maxAttempts = 4)
            kandidaterMed4 shouldHaveSize 2
        }

        @Test
        @DisplayName("Skal finne UNDER_BEHANDLING med NULL sisteForsoekTidspunkt (kritisk edge case)")
        fun `skal finne UNDER_BEHANDLING med NULL sisteForsoekTidspunkt`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))
            val innsending = innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = skjema,
                    status = InnsendingStatus.UNDER_BEHANDLING,
                    sisteForsoekTidspunkt = null  // App krasjet før første oppdatering
                )
            )

            val grense = Instant.now().minus(5, ChronoUnit.MINUTES)
            val kandidater = innsendingRepository.findRetryKandidater(grense, maxAttempts = 5)

            kandidater shouldHaveSize 1
            kandidater[0].id shouldBe innsending.id
        }

        @Test
        @DisplayName("Skal finne UNDER_BEHANDLING med gammel sisteForsoekTidspunkt")
        fun `skal finne UNDER_BEHANDLING med gammel sisteForsoekTidspunkt`() {
            val gammelSisteForsoekTidspunkt = Instant.now().minus(10, ChronoUnit.MINUTES)
            val innsending = innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = opprettSkjema(),
                    status = InnsendingStatus.UNDER_BEHANDLING,
                    sisteForsoekTidspunkt = gammelSisteForsoekTidspunkt
                )
            )

            val grense = Instant.now().minus(5, ChronoUnit.MINUTES)
            val kandidater = innsendingRepository.findRetryKandidater(grense, maxAttempts = 5)

            kandidater shouldHaveSize 1
            kandidater[0].id shouldBe innsending.id
        }

        @Test
        @DisplayName("Skal IKKE finne UNDER_BEHANDLING med fersk sisteForsoekTidspunkt")
        fun `skal ikke finne UNDER_BEHANDLING med fersk sisteForsoekTidspunkt`() {
            val ferskSisteForsoekTidspunkt = Instant.now().minus(1, ChronoUnit.MINUTES)
            innsendingRepository.save(
                innsendingMedDefaultVerdier(
                    skjema = opprettSkjema(),
                    status = InnsendingStatus.UNDER_BEHANDLING,
                    sisteForsoekTidspunkt = ferskSisteForsoekTidspunkt
                )
            )

            val grense = Instant.now().minus(5, ChronoUnit.MINUTES)
            val kandidater = innsendingRepository.findRetryKandidater(grense, maxAttempts = 5)

            kandidater.shouldBeEmpty()
        }
    }

    @Nested
    @DisplayName("findBySkjema")
    inner class FindBySkjemaTests {

        @Test
        @DisplayName("Skal finne innsending for skjema")
        fun `skal finne innsending for skjema`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))
            val innsending = innsendingRepository.save(innsendingMedDefaultVerdier(skjema = skjema))

            val funnet = innsendingRepository.findBySkjema(skjema)

            funnet?.id shouldBe innsending.id
        }

        @Test
        @DisplayName("Skal returnere null når innsending ikke finnes")
        fun `skal returnere null når innsending ikke finnes`() {
            val skjema = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))

            val funnet = innsendingRepository.findBySkjema(skjema)

            funnet shouldBe null
        }
    }

    private fun opprettSkjema() = skjemaRepository.save(skjemaMedDefaultVerdier(status = SkjemaStatus.SENDT))
}
