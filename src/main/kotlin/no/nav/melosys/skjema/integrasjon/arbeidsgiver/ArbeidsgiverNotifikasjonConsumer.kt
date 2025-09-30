package no.nav.melosys.skjema.integrasjon.arbeidsgiver

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.GraphQLRequest
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.GraphQLResponse
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.NyBeskjedResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.UUID

private val log = KotlinLogging.logger { }

data class BeskjedRequest(
    val virksomhetsnummer: String,
    val tekst: String,
    val lenke: String,
    val eksternId: String = UUID.randomUUID().toString(),
)

@Component
class ArbeidsgiverNotifikasjonConsumer(
    private val arbeidsgiverNotifikasjonClient: WebClient,
    @Value("\${arbeidsgiver.notifikasjon.merkelapp}") private val merkelapp: String,
    @Value("\${arbeidsgiver.notifikasjon.ressursId}") private val ressursId: String,
) {

    private val nyBeskjedMutation: String by lazy {
        ClassPathResource("graphql/opprett-ny-beskjed.graphql").inputStream.bufferedReader().use { it.readText() }
    }

    fun opprettBeskjed(request: BeskjedRequest): String {
        log.info { "Oppretter ny beskjed for virksomhet ${request.virksomhetsnummer} med eksternId ${request.eksternId}" }

        val graphQLRequest = GraphQLRequest(
            query = nyBeskjedMutation,
            variables = mapOf(
                "eksternId" to request.eksternId,
                "virksomhetsnummer" to request.virksomhetsnummer,
                "lenke" to request.lenke,
                "tekst" to request.tekst,
                "merkelapp" to merkelapp,
                "ressursId" to ressursId,
            )
        )

        val response = arbeidsgiverNotifikasjonClient.post()
            .uri("/api/graphql")
            .bodyValue(graphQLRequest)
            .retrieve()
            .bodyToMono<GraphQLResponse<NyBeskjedResponse>>()
            .block()

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