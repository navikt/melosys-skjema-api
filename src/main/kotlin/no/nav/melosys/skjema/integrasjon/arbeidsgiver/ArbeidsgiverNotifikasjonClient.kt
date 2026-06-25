package no.nav.melosys.skjema.integrasjon.arbeidsgiver

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.BeskjedRequest
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.GraphQLRequest
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.GraphQLResponse
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.NyBeskjedResponse
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.NyBeskjedVariables
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

private val log = KotlinLogging.logger { }

@Component
class ArbeidsgiverNotifikasjonClient(
    private val arbeidsgiverNotifikasjonRestClient: RestClient,
    @param:Value("\${arbeidsgiver.notifikasjon.merkelapp}") private val merkelapp: String,
    @param:Value("\${arbeidsgiver.notifikasjon.ressursId}") private val ressursId: String,
) {

    private val nyBeskjedMutation: String by lazy {
        ClassPathResource("graphql/opprett-ny-beskjed.graphql").inputStream.bufferedReader().use { it.readText() }
    }

    fun opprettBeskjed(request: BeskjedRequest): String {
        log.info { "Oppretter ny beskjed for virksomhet ${request.virksomhetsnummer} med eksternId ${request.eksternId}" }

        val variables = NyBeskjedVariables(
            request.eksternId,
            request.virksomhetsnummer,
            request.lenke,
            request.tekst,
            merkelapp,
            ressursId,
        )

        val graphQLRequest = GraphQLRequest(
            query = nyBeskjedMutation,
            variables = variables
        )

        val response = arbeidsgiverNotifikasjonRestClient.post()
            .uri("/api/graphql")
            .body(graphQLRequest)
            .retrieve()
            .body(object : org.springframework.core.ParameterizedTypeReference<GraphQLResponse<NyBeskjedResponse>>() {})

        if (response?.errors?.isNotEmpty() == true) {
            val errorMessage = response.errors.joinToString(", ") { it.message }
            throw RuntimeException("GraphQL feil ved opprettelse av beskjed: $errorMessage")
        }

        val beskjedResult = response?.data?.nyBeskjed
        return when (beskjedResult?.__typename) {
            "NyBeskjedVellykket" -> {
                log.info { "Beskjed opprettet med id: ${beskjedResult.id}" }
                beskjedResult.id ?: throw RuntimeException("Manglende id i vellykket respons")
            }
            "Error" -> {
                throw RuntimeException("Feil ved opprettelse av beskjed: ${beskjedResult.feilmelding}")
            }
            else -> {
                throw RuntimeException("Ukjent respons type: ${beskjedResult?.__typename}")
            }
        }
    }
}