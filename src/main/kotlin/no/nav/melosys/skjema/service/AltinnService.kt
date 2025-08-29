package no.nav.melosys.skjema.service

import no.nav.melosys.skjema.dto.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class AltinnService(
    private val restTemplate: RestTemplate,
    @Value("\${altinn.base-url}") private val altinnBaseUrl: String
) {

    fun getRepresentasjoner(request: RepresentasjonerRequest): RepresentasjonerResponse {
        val url = "$altinnBaseUrl/m2m/altinn-tilganger"
        
        val altinnRequest = AltinnRequest(
            fnr = request.fnr,
            filter = AltinnFilter(
                altinn2Tilganger = listOf("4936:1"), //TODO endre til det vi får opprettet. Det er sannsynlig at vi ikke får opprettet altinn 3 med det første
                altinn3Tilganger = listOf("nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger")
            )
        )
        
        val altinnResponse = restTemplate.postForObject(url, altinnRequest, AltinnResponse::class.java)
        
        val orgnrList = altinnResponse?.tilgangTilOrgNr?.get("tilgang1") ?: emptyList() //TODO endre til vår tilgang
        
        return RepresentasjonerResponse(
            fnr = emptyList(),
            orgnr = orgnrList
        )
    }
}