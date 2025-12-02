package no.nav.melosys.skjema.controller

import no.nav.melosys.skjema.integrasjon.ereg.EregService
import no.nav.melosys.skjema.integrasjon.ereg.dto.Organisasjon
import no.nav.melosys.skjema.integrasjon.ereg.dto.OrganisasjonMedJuridiskEnhet
import no.nav.melosys.skjema.service.RateLimitOperationType
import no.nav.melosys.skjema.service.RateLimiterService
import no.nav.melosys.skjema.service.exception.RateLimitExceededException
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/ereg")
@Protected
class EregController(
    private val eregService: EregService,
    private val rateLimiterService: RateLimiterService
) {

    /**
     * Henter organisasjon med juridisk enhet fra Enhetsregisteret.
     * Inkluderer rate limiting (se RateLimitConfig for grenser).
     *
     * @param orgnummer 9-sifret organisasjonsnummer
     * @return Organisasjon med juridisk enhet, inkludert navn, adresse, enhetstype, etc.
     * @throws RateLimitExceededException hvis brukeren har overskredet rate limit
     */
    @GetMapping("/organisasjon-med-juridisk-enhet/{orgnummer}")
    fun hentOrganisasjonMedJuridiskEnhet(
        @PathVariable orgnummer: String
    ): ResponseEntity<OrganisasjonMedJuridiskEnhet> = withRateLimit {
        val organisasjon = eregService.hentOrganisasjonMedJuridiskEnhet(orgnummer)
        ResponseEntity.ok(organisasjon)
    }

    /**
     * Henter organisasjon fra Enhetsregisteret uten hierarki.
     * Inkluderer rate limiting (se RateLimitConfig for grenser).
     *
     * @param orgnummer 9-sifret organisasjonsnummer
     * @return Organisasjon uten hierarki
     * @throws RateLimitExceededException hvis brukeren har overskredet rate limit
     */
    @GetMapping("/organisasjon/{orgnummer}")
    fun hentOrganisasjon(
        @PathVariable orgnummer: String
    ): ResponseEntity<Organisasjon> = withRateLimit {
        val organisasjon = eregService.hentOrganisasjon(orgnummer)
        ResponseEntity.ok(organisasjon)
    }

    private fun <T> withRateLimit(
        block: () -> T
    ): T {
        val userId = SubjectHandler.getInstance().getUserID()
        val rateLimitOperationType = RateLimitOperationType.ORGANISASJONSSOK
        if (rateLimiterService.isRateLimited(userId, rateLimitOperationType)) {
            throw RateLimitExceededException(rateLimitOperationType)
        }
        return block()
    }
}
