package no.nav.melosys.skjema.integrasjon.ereg

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.ereg.dto.Organisasjon
import no.nav.melosys.skjema.integrasjon.ereg.exception.OrganisasjonEksistererIkkeException
import org.springframework.cache.annotation.Cacheable
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

private val log = KotlinLogging.logger { }

@Component
class EregClient(
    private val eregRestClient: RestClient
) {

    @Retryable(
        retryFor = [HttpServerErrorException::class, ResourceAccessException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000L, multiplier = 2.0, random = true)
    )
    @Cacheable("ereg", key = "#orgnummer + '_' + #inkluderHierarki")
    fun hentOrganisasjon(orgnummer: String, inkluderHierarki: Boolean = false): Organisasjon {
        log.info { "Henter organisasjon fra EREG: $orgnummer" }

        return eregRestClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/v2/organisasjon/{orgnummer}")
                    .queryParam("inkluderHierarki", inkluderHierarki)
                    .build(orgnummer)
            }
            .retrieve()
            .onStatus({ it.value() == 404 }) { _, _ ->
                throw OrganisasjonEksistererIkkeException(orgnummer)
            }
            .body<Organisasjon>()
            ?: error("Fikk null response fra EREG for orgnummer: $orgnummer")
    }
}
