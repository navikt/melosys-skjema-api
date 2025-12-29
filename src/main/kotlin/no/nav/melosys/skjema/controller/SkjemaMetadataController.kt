package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.ServletResponse
import no.nav.melosys.skjema.service.UtsendtArbeidstakerService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import no.nav.melosys.skjema.dto.Representasjonstype

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/api/skjema")
@Tag(name = "Skjema", description = "placeholder")
@Protected
class SkjemaMetadataController(
    private val utsendtArbeidstakerService: UtsendtArbeidstakerService,
) {

    @GetMapping("/{id}/representasjonstype")
    @Operation(summary = "Hent lightweight metadata for routing")
    @ApiResponse(responseCode = "200", description = "Metadata hentet")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang")
    @ApiResponse(responseCode = "404", description = "Skjema ikke funnet")
    fun getSkjemaMetadata(@PathVariable id: UUID): ResponseEntity<Representasjonstype> {
        log.info { "Henter representasjonstype for skjema: $id" }

        return ResponseEntity.ok(utsendtArbeidstakerService.getRepresentasjonstype(id))
    }

}