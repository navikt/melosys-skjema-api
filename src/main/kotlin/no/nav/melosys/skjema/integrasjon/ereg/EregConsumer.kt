package no.nav.melosys.skjema.integrasjon.ereg

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.ereg.dto.Organisasjon
import no.nav.melosys.skjema.integrasjon.ereg.exception.OrganisasjonEksistererIkkeException
import no.nav.melosys.skjema.integrasjon.felles.WebClientConfig.defaultRetry
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger { }

@Component
class EregConsumer(
    private val eregClient: WebClient
) {

    @Cacheable("ereg", key = "#orgnummer + '_' + #inkluderHierarki")
    fun hentOrganisasjon(orgnummer: String, inkluderHierarki: Boolean = false): Organisasjon {
        log.info { "Henter organisasjon fra EREG: $orgnummer" }

        return eregClient.get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/v2/organisasjon/{orgnummer}")
                    .queryParam("inkluderHierarki", inkluderHierarki)
                    .build(orgnummer)
            }
            .retrieve()
            .onStatus({ it.value() == 404 }) {
                Mono.error(OrganisasjonEksistererIkkeException(orgnummer))
            }
            .bodyToMono(Organisasjon::class.java)
            .retryWhen(defaultRetry())
            .block()
            ?: error("Fikk null response fra EREG for orgnummer: $orgnummer")
    }
}
