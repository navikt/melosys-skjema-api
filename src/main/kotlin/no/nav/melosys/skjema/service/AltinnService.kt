package no.nav.melosys.skjema.service

import no.nav.melosys.skjema.dto.AltinnResponse
import no.nav.melosys.skjema.dto.RepresentasjonerRequest
import no.nav.melosys.skjema.dto.RepresentasjonerResponse
import no.nav.melosys.skjema.dto.RessursRegisterResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class AltinnService(
    private val restTemplate: RestTemplate,
    @Value("\${altinn.base-url}") private val altinnBaseUrl: String,
    @Value("\${altinn.tilganger-endpoint}") private val tilgangerEndpoint: String,
    @Value("\${altinn.resource-registry-url}") private val resourceRegistryUrl: String
) {

    companion object {
        private val PAAREVDE_TILGANGER = listOf(
            "4936:1",
            "tilgang1"
        )
    }

    fun getRepresentasjoner(request: RepresentasjonerRequest): RepresentasjonerResponse {
        val url = "$altinnBaseUrl$tilgangerEndpoint"
        
        val altinnResponse = restTemplate.postForObject(url, request, AltinnResponse::class.java)
        
        val tilgangerForRessursRespons = restTemplate.getForObject(resourceRegistryUrl, RessursRegisterResponse::class.java)
        val tilgangerForRessurs = tilgangerForRessursRespons?.data?.map { it.type } ?: emptyList()
        
        val alleTilganger = PAAREVDE_TILGANGER + tilgangerForRessurs
        
        val filtrertOrgnrListe = altinnResponse?.orgNrTilTilganger
            ?.filter { (_, tilganger) ->
                tilganger.any { tilgang -> alleTilganger.contains(tilgang) }
            }
            ?.keys?.toList() ?: emptyList()
        
        return RepresentasjonerResponse(
            fnr = request.fnr,
            orgnr = filtrertOrgnrListe
        )
    }
}