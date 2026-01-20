package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.skjema.entity.Skjema
import no.nav.melosys.skjema.service.M2MSkjemaService
import no.nav.melosys.skjema.sikkerhet.M2MReadSkjemadata
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/m2m/api/skjema")
@Tag(name = "M2M Skjema", description = "Machine-to-machine endepunkter for skjemadata")
class M2MSkjemaController(
    private val m2mSkjemaService: M2MSkjemaService
) {

    @GetMapping("/{id}/data")
    @M2MReadSkjemadata
    @Operation(summary = "Hent skjema for gitt ID (M2M)")
    @ApiResponse(responseCode = "200", description = "Skjema hentet")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang")
    @ApiResponse(responseCode = "404", description = "Skjema ikke funnet")
    fun getSkjema(@PathVariable id: UUID): ResponseEntity<Skjema> {
        log.info { "M2M: Henter skjema med id: $id" }
        return ResponseEntity.ok(m2mSkjemaService.hentSkjemaData(id))
    }
}
