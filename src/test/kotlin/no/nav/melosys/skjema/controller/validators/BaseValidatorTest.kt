package no.nav.melosys.skjema.controller.validators

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import jakarta.validation.Validator
import no.nav.melosys.skjema.integrasjon.ereg.EregService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [BaseValidatorTest.TestConfig::class])
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseValidatorTest {
    @Autowired
    protected lateinit var validator: Validator

    @MockkBean
    protected lateinit var eregService: EregService

    @BeforeEach
    fun setup() {
        every { eregService.organisasjonsnummerEksisterer(any()) } returns true
    }

    @Configuration
    class TestConfig {
        @Bean
        fun validator(): Validator = LocalValidatorFactoryBean()
    }
}
