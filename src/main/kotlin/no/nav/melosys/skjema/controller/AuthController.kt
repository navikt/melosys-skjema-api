package no.nav.melosys.skjema.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "placeholder")
class AuthController {

    @GetMapping("/representasjoner")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun getRepresentasjoner(): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }
}