package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.skjema.service.NotificationService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/api/skjema")
@Tag(name = "Skjema", description = "placeholder")
@Protected
class SkjemaController(
    private val notificationService: NotificationService
) {

    @GetMapping
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun listSkjemaer(): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @PostMapping
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun createSkjema(@RequestBody skjema: Any): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun getSkjema(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @PutMapping("/{id}")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun updateSkjema(@PathVariable id: String, @RequestBody skjema: Any): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun deleteSkjema(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun submitSkjema(@PathVariable id: String): ResponseEntity<Any> {
        log.info { "Submitting skjema med id: $id" }

        try {
            notificationService.sendNotificationToArbeidstaker(id, "Skjema har blitt sendt til behandling") //TODO finn ut hva som faktisk skal stå her
            log.info { "Notifikasjon sendt for skjema med id: $id" }
            return ResponseEntity.ok().build()
        } catch (e: Exception) {
            log.error(e) { "Feil ved sending av notifikasjon for skjema med id: $id" }
            return ResponseEntity.ok().build()
        }
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun generatePdf(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }
}