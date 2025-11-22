package no.nav.melosys.skjema.integrasjon.pdl

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.felles.graphql.GraphQLError
import no.nav.melosys.skjema.integrasjon.felles.graphql.GraphQLRequest
import no.nav.melosys.skjema.integrasjon.felles.graphql.GraphQLResponse
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlHentPersonResponse
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlPerson
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

private val log = KotlinLogging.logger { }

@Component
class PdlConsumer(
    private val pdlClient: WebClient
) {

    /**
     * Henter person fra PDL med navn og fødselsdato.
     * Resultatet caches i 5 minutter for å redusere belastning på PDL.
     *
     * @param ident Fødselsnummer eller d-nummer
     * @return PdlPerson med navn og fødselsdato
     * @throws IllegalArgumentException hvis person ikke finnes eller response er ugyldig
     */
    @Cacheable(value = ["pdl-person"], key = "#ident")
    fun hentPerson(ident: String): PdlPerson {
        log.info { "Henter person fra PDL" }

        val graphQLRequest = GraphQLRequest(
            query = PdlQuery.HENT_PERSON_NAVN_FODSELSDATO,
            variables = mapOf("ident" to ident)
        )

        val response = pdlClient.post()
            .header("Nav-Call-Id", UUID.randomUUID().toString())
            .bodyValue(graphQLRequest)
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<GraphQLResponse<PdlHentPersonResponse>>() {})
            .retryWhen(no.nav.melosys.skjema.integrasjon.felles.WebClientConfig.defaultRetry(maxAttempts = 3, backoffDurationMillis = 1000))
            .block()

        håndterFeil(response)

        return response?.data?.hentPerson
            ?: throw IllegalArgumentException("Fant ikke person i PDL")
    }

    private fun håndterFeil(response: GraphQLResponse<PdlHentPersonResponse>?) {
        if (response == null) {
            throw RuntimeException("Respons fra PDL er null")
        }

        if (!response.errors.isNullOrEmpty()) {
            if (response.errors.any { it.isNotFound() }) {
                throw IllegalArgumentException("Fant ikke ident i PDL")
            }
            throw RuntimeException("Kall mot PDL feilet: ${response.errors}")
        }
    }

    private fun GraphQLError.isNotFound(): Boolean {
        return hasExtension() && extensions?.hasCode(PdlFeilkode.NOT_FOUND) == true
    }
}
