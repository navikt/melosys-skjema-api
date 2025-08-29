package no.nav.melosys.skjema.service

import no.nav.melosys.skjema.dto.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class AltinnService(private val restTemplate: RestTemplate) {

    fun getRepresentasjoner(request: RepresentasjonerRequest): RepresentasjonerResponse {
        val url = "http://localhost:8083/m2m/altinn-tilganger"
        
        val altinnRequest = AltinnRequest(
            fnr = request.fnr,
            filter = AltinnFilter(
                altinn2Tilganger = listOf("4936:1"), //TODO endre til det vi får opprettet. Det er sannsynlig at vi ikke får opprettet altinn 3 med det første
                altinn3Tilganger = listOf("nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger")
            )
        )
        
        val altinnResponse = restTemplate.postForObject(url, altinnRequest, AltinnResponse::class.java)
        
        // Extract organization numbers for "tilgang1" permission
        val orgnrList = altinnResponse?.tilgangTilOrgNr?.get("tilgang1") ?: emptyList()
        
        return RepresentasjonerResponse(
            fnr = emptyList(),
            orgnr = orgnrList
        )
    }
}