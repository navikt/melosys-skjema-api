package no.nav.melosys.skjema.integrasjon.pdl

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import no.nav.melosys.skjema.integrasjon.felles.graphql.GraphQLError
import no.nav.melosys.skjema.integrasjon.felles.graphql.GraphQLRequest
import no.nav.melosys.skjema.integrasjon.felles.graphql.GraphQLResponse
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlHentPersonBolkResponse
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlHentPersonResponse
import no.nav.melosys.skjema.integrasjon.pdl.dto.PdlPerson
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient

private val log = KotlinLogging.logger { }

@Component
class PdlClient(
    private val pdlRestClient: RestClient
) {

    /**
     * Henter person fra PDL med navn og fødselsdato.
     * Resultatet caches i 5 minutter for å redusere belastning på PDL.
     *
     * @param ident Fødselsnummer eller d-nummer
     * @return PdlPerson med navn og fødselsdato
     * @throws IllegalArgumentException hvis person ikke finnes eller response er ugyldig
     */
    @Retryable(
        retryFor = [HttpServerErrorException::class, ResourceAccessException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000L, multiplier = 2.0, random = true)
    )
    @Cacheable(value = ["pdl-person"], key = "#ident")
    fun hentPerson(ident: String): PdlPerson {
        log.debug { "Henter person fra PDL" }

        val graphQLRequest = GraphQLRequest(
            query = PdlQuery.HENT_PERSON_NAVN_FODSELSDATO,
            variables = mapOf("ident" to ident)
        )

        val response = pdlRestClient.post()
            .header("Nav-Call-Id", UUID.randomUUID().toString())
            .body(graphQLRequest)
            .retrieve()
            .body(object : ParameterizedTypeReference<GraphQLResponse<PdlHentPersonResponse>>() {})

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

    /**
     * Henter flere personer fra PDL i én request (bulk).
     * Returnerer et map hvor key er fnr og value er PdlPerson.
     * Personer som ikke finnes eller har ugyldig ident blir ikke inkludert i resultatet.
     * Resultatet caches basert på den sorterte listen av identer for å gi cache-treff på samme sett av personer.
     *
     * @param identer Liste med fødselsnummer/d-nummer
     * @return Map med fnr som key og PdlPerson som value (kun for personer som ble funnet)
     */
    @Retryable(
        retryFor = [HttpServerErrorException::class, ResourceAccessException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000L, multiplier = 2.0, random = true)
    )
    @Cacheable(value = ["pdl-personer-bulk"], keyGenerator = "pdlBolkKeyGenerator")
    fun hentPersonerBolk(identer: List<String>): Map<String, PdlPerson> {
        if (identer.isEmpty()) {
            return emptyMap()
        }

        log.debug { "Henter ${identer.size} personer fra PDL med bulk-query" }

        val graphQLRequest = GraphQLRequest(
            query = PdlQuery.HENT_PERSON_BOLK,
            variables = mapOf("identer" to identer)
        )

        val response = pdlRestClient.post()
            .header("Nav-Call-Id", UUID.randomUUID().toString())
            .body(graphQLRequest)
            .retrieve()
            .body(object : ParameterizedTypeReference<GraphQLResponse<PdlHentPersonBolkResponse>>() {})

        if (response == null) {
            throw RuntimeException("Respons fra PDL bulk-query er null")
        }

        if (!response.errors.isNullOrEmpty()) {
            throw RuntimeException("Kall mot PDL (bulk) feilet: ${response.errors}")
        }

        val entries = response.data?.hentPersonBolk ?: emptyList()

        // Filtrer ut kun personer som har status "ok" og faktisk person-data
        return entries
            .filter { it.code == "ok" && it.person != null }
            .associate { it.ident to it.person!! }
            .also { result ->
                log.debug { "Hentet ${result.size} av ${identer.size} personer fra PDL" }
                val notFound = entries.filter { it.code == "not_found" }
                if (notFound.isNotEmpty()) {
                    log.debug { "Fant ikke ${notFound.size} personer i PDL" }
                }
            }
    }

    private fun GraphQLError.isNotFound(): Boolean {
        return hasExtension() && extensions?.hasCode(PdlFeilkode.NOT_FOUND) == true
    }
}
