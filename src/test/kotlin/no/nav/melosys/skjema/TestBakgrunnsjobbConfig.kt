package no.nav.melosys.skjema

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.core.task.TaskExecutor

/**
 * Kjører @Async synkront i tester.
 *
 * Med trådpool fortsetter AFTER_COMMIT-prosesseringen av innsending etter at HTTP-responsen
 * er sendt, altså parallelt med neste tests databaseopprydding. De to transaksjonene tar da
 * låser på skjema og innsending i motsatt rekkefølge, og Postgres avbryter den ene med
 * deadlock. Synkron kjøring gjør testene deterministiske.
 */
@Configuration
class TestBakgrunnsjobbConfig {

    @Bean
    @Primary
    fun synkronTaskExecutor(): TaskExecutor = SyncTaskExecutor()
}
