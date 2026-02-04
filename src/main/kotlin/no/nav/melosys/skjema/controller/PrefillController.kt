package no.nav.melosys.skjema.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/preutfyll")
@Tag(name = "Preutfyll", description = "placeholder")
@Protected
class PrefillController {

    @PostMapping("/person")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun getPersonData(@RequestBody request: Any): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @GetMapping("/org/{orgnr}")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun getOrgData(@PathVariable orgnr: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }
}