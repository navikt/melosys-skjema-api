package no.nav.melosys.skjema.integrasjon.aareg

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.aareg.dto.Arbeidsforhold
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

private val log = KotlinLogging.logger { }

const val ARBEIDSFORHOLDTYPE_ORDINAERT = "ordinaertArbeidsforhold"

/**
 * Klient mot AAREG v2 (`/api/v2/arbeidstaker/arbeidsforhold`). Henter også avsluttede arbeidsforhold,
 * siden søknadsperioden kan ligge tilbake i tid (AAREG gir bare aktive og fremtidige som standard).
 */
@Component
class AaregClient(
    private val aaregRestClient: RestClient
) {

    @Retryable(
        retryFor = [HttpServerErrorException::class, ResourceAccessException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000L, multiplier = 2.0, random = true)
    )
    fun hentOrdinaereArbeidsforhold(fnr: String): List<Arbeidsforhold> {
        log.info { "Henter ordinære arbeidsforhold fra AAREG" }

        return aaregRestClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/api/v2/arbeidstaker/arbeidsforhold")
                    .queryParam("arbeidsforholdtype", ARBEIDSFORHOLDTYPE_ORDINAERT)
                    .queryParam("arbeidsforholdstatus", "AKTIV,AVSLUTTET,FREMTIDIG")
                    .build()
            }
            .header("Nav-Personident", fnr)
            .retrieve()
            .body<List<Arbeidsforhold>>()
            ?: error("Fikk null response fra AAREG")
    }
}
