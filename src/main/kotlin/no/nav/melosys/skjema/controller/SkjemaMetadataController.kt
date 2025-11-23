package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.skjema.exception.AccessDeniedException
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.service.AltinnService
import no.nav.melosys.skjema.service.UtsendtArbeidstakerService
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/api/skjema")
@Tag(name = "Skjema", description = "placeholder")
@Protected
class SkjemaMetadataController(
    private val utsendtArbeidstakerService: UtsendtArbeidstakerService,
    private val altinnService: AltinnService,
    private val reprService: ReprService,
    private val subjectHandler: SubjectHandler,
) {

    @GetMapping("/{id}/metadata")
    @Operation(summary = "Hent lightweight metadata for routing")
    @ApiResponse(responseCode = "200", description = "Metadata hentet")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang")
    @ApiResponse(responseCode = "404", description = "Skjema ikke funnet")
    fun getSkjemaMetadata(@PathVariable id: UUID): ResponseEntity<Map<String, Any>> {
        log.info { "Henter metadata for skjema: $id" }

        // Valider at bruker har tilgang (enten arbeidsgiver eller arbeidstaker)
        try {
            validerArbeidsgiverTilgang(id)
        } catch (e: AccessDeniedException) {
            validerArbeidstakerTilgang(id)
        }

        val utsendtSkjema = utsendtArbeidstakerService.hentSkjema(id)
        val metadata = mapOf(
            "representasjonstype" to utsendtSkjema.metadata.representasjonstype.name
        )
        return ResponseEntity.ok(metadata)
    }


    // Helper metoder for tilgangskontroll
    private fun validerArbeidsgiverTilgang(skjemaId: UUID) {
        val utsendtSkjema = utsendtArbeidstakerService.hentSkjema(skjemaId)
        val orgnr = utsendtSkjema.orgnr

        if (orgnr == null || !altinnService.harBrukerTilgang(orgnr)) {
            throw AccessDeniedException("Ingen tilgang til arbeidsgiver-del")
        }
    }

    private fun validerArbeidstakerTilgang(skjemaId: UUID) {
        val utsendtSkjema = utsendtArbeidstakerService.hentSkjema(skjemaId)
        val currentUser = subjectHandler.getUserID()
        val fnr = utsendtSkjema.fnr

        val harTilgang = fnr == currentUser ||
                (utsendtSkjema.metadata.fullmektigFnr == currentUser &&
                        fnr != null &&
                        reprService.harSkriverettigheterForMedlemskap(fnr))

        if (!harTilgang) {
            throw AccessDeniedException("Ingen tilgang til arbeidstaker-del")
        }
    }
}