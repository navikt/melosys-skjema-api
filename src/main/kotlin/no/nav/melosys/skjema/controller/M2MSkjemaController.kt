package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import no.nav.melosys.skjema.service.M2MSkjemaService
import no.nav.melosys.skjema.sikkerhet.M2MReadSkjemadata
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerSkjemaM2MDto
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/m2m/api/skjema")
@Tag(name = "M2M Skjema", description = "Machine-to-machine endepunkter for skjemadata")
class M2MSkjemaController(
    private val m2mSkjemaService: M2MSkjemaService
) {

    @GetMapping("/utsendt-arbeidstaker/{id}/data")
    @M2MReadSkjemadata
    @Operation(summary = "Hent skjema for gitt ID (M2M)")
    @ApiResponse(responseCode = "200", description = "Skjema hentet")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang")
    @ApiResponse(responseCode = "404", description = "Skjema ikke funnet")
    fun getSkjema(@PathVariable id: UUID): ResponseEntity<UtsendtArbeidstakerSkjemaM2MDto> {
        log.info { "M2M: Henter skjema med id: $id" }
        return ResponseEntity.ok(m2mSkjemaService.hentUtsendtArbeidstakerSkjemaData(id))
    }

    @GetMapping("/{id}/pdf")
    @M2MReadSkjemadata
    @Operation(summary = "Hent PDF for innsendt skjema (M2M)")
    @ApiResponse(responseCode = "200", description = "PDF generert")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang")
    @ApiResponse(responseCode = "404", description = "Skjema ikke funnet eller ikke innsendt")
    fun getPdf(@PathVariable id: UUID): ResponseEntity<ByteArray> {
        log.info { "M2M: Genererer PDF for skjema med id: $id" }
        val pdf = m2mSkjemaService.hentPdfForSkjema(id)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf)
    }
}
