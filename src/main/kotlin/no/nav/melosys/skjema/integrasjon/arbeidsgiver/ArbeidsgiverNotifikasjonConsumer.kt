package no.nav.melosys.skjema.integrasjon.arbeidsgiver

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.GraphQLRequest
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.GraphQLResponse
import no.nav.melosys.skjema.integrasjon.arbeidsgiver.dto.NyBeskjedResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.UUID

private val log = KotlinLogging.logger { }

@Component
class ArbeidsgiverNotifikasjonConsumer(
    private val arbeidsgiverNotifikasjonClient: WebClient,
    @Value("\${arbeidsgiver.notifikasjon.merkelapp}") private val merkelapp: String
) {

    companion object {
        private const val NY_BESKJED_MUTATION = """
            mutation OpprettNyBeskjed(
              ${'$'}eksternId: String!
              ${'$'}virksomhetsnummer: String!
              ${'$'}lenke: String!
              ${'$'}tekst: String!
              ${'$'}merkelapp: String!
            ) {
              nyBeskjed(nyBeskjed: {
                metadata: {
                  eksternId: ${'$'}eksternId
                  virksomhetsnummer: ${'$'}virksomhetsnummer
                }
                mottakere: [{
                  altinn: {
                    altinnressurs: {
                      ressursId: "team-fager"
                    }
                  }
                }]
                notifikasjon: {
                  merkelapp: ${'$'}merkelapp
                  tekst: ${'$'}tekst
                  lenke: ${'$'}lenke
                }
              }) {
                __typename
                ... on NyBeskjedVellykket {
                  id
                }
                ... on Error {
                  feilmelding
                }
              }
            }
        """
    }

    fun opprettBeskjed(
        virksomhetsnummer: String,
        tekst: String,
        lenke: String,
        eksternId: String = UUID.randomUUID().toString()
    ): String {
        log.info { "Oppretter ny beskjed for virksomhet $virksomhetsnummer med eksternId $eksternId" }

        val graphQLRequest = GraphQLRequest(
            query = NY_BESKJED_MUTATION,
            variables = mapOf(
                "eksternId" to eksternId,
                "virksomhetsnummer" to virksomhetsnummer,
                "lenke" to lenke,
                "tekst" to tekst,
                "merkelapp" to merkelapp
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