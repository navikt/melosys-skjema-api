package no.nav.melosys.skjema

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation


@SpringBootApplication
@EnableJwtTokenValidation
class MelosysSkjemaApiApplication

fun main(args: Array<String>) {
    runApplication<MelosysSkjemaApiApplication>(*args)
}
