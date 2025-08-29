package no.nav.melosys.skjema.service

import no.nav.melosys.skjema.dto.AltinnResponse
import no.nav.melosys.skjema.dto.RepresentasjonerRequest
import no.nav.melosys.skjema.dto.RepresentasjonerResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class AltinnService(
    private val restTemplate: RestTemplate,
    @Value("\${altinn.base-url}") private val altinnBaseUrl: String,
    @Value("\${altinn.tilganger-endpoint}") private val tilgangerEndpoint: String
) {

    fun getRepresentasjoner(request: RepresentasjonerRequest): RepresentasjonerResponse {
        val url = "$altinnBaseUrl$tilgangerEndpoint"
        
        val altinnResponse = restTemplate.postForObject(url, request, AltinnResponse::class.java)
        
        val orgnrList = altinnResponse?.tilgangTilOrgNr?.get("tilgang1") ?: emptyList() //TODO endre til vår tilgang
        
        return RepresentasjonerResponse(
            fnr = request.fnr,
            orgnr = orgnrList
        )
    }
}