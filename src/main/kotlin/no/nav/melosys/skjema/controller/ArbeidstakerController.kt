package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import no.nav.melosys.skjema.controller.dto.VerifiserPersonRequest
import no.nav.melosys.skjema.controller.dto.VerifiserPersonResponse
import no.nav.melosys.skjema.integrasjon.pdl.PdlService
import no.nav.melosys.skjema.service.RateLimitOperationType
import no.nav.melosys.skjema.service.RateLimiterService
import no.nav.melosys.skjema.service.exception.RateLimitExceededException
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/api/arbeidstaker")
@Tag(name = "Arbeidstaker", description = "Endepunkter for arbeidstaker-verifisering")
@Protected
class ArbeidstakerController(
    private val pdlService: PdlService,
    private val rateLimiterService: RateLimiterService
) {

    @PostMapping("/verifiser-person")
    @Operation(
        summary = "Verifiser person uten fullmakt",
        description = "Verifiserer at en person med gitt fødselsnummer og navn eksisterer i PDL. " +
                "Brukes for arbeidstakere uten fullmakt. " +
                "Returnerer 400 hvis fødselsnummer og navn ikke matcher. " +
                "Inkluderer rate limiting (se RateLimitConfig for grenser)."
    )
    @ApiResponse(responseCode = "200", description = "Person verifisert - returnerer navn og fødselsdato")
    @ApiResponse(responseCode = "400", description = "Ugyldig input eller finner ikke person med oppgitt fødselsnummer og navn")
    @ApiResponse(responseCode = "401", description = "Ikke autentisert")
    @ApiResponse(responseCode = "429", description = "Rate limit overskredet")
    fun verifiserPerson(@Valid @RequestBody request: VerifiserPersonRequest): ResponseEntity<VerifiserPersonResponse> {
        val userId = SubjectHandler.getInstance().getUserID()
        if (rateLimiterService.isRateLimited(userId, RateLimitOperationType.PERSONVERIFISERING)) {
            throw RateLimitExceededException(RateLimitOperationType.PERSONVERIFISERING)
        }

        log.info { "Verifiserer person" }

        val (navn, fodselsdato) = pdlService.verifiserOgHentPerson(
            fodselsnummer = request.fodselsnummer,
            navn = request.navn
        )

        // TODO: Legg til auditlogs av personverifiseringen

        return ResponseEntity.ok(
            VerifiserPersonResponse(
                navn = navn,
                fodselsdato = fodselsdato
            )
        )
    }
}
