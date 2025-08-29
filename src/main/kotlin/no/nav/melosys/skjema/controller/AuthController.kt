package no.nav.melosys.skjema.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.skjema.dto.RepresentasjonerRequest
import no.nav.melosys.skjema.dto.RepresentasjonerResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "placeholder")
class AuthController {

    @PostMapping("/representasjoner")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun getRepresentasjoner(@RequestBody request: RepresentasjonerRequest): ResponseEntity<RepresentasjonerResponse> {
        val response = RepresentasjonerResponse(
            fnr = emptyList(),
            orgnr = emptyList()
        )
        return ResponseEntity.ok(response)
    }
}