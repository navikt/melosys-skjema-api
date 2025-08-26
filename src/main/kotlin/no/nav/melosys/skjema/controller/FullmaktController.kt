package no.nav.melosys.skjema.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/fullmakt")
@Tag(name = "Fullmakt", description = "placeholder")
class FullmaktController {

    @PostMapping
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun requestFullmakt(@RequestBody request: Any): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun getFullmakt(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{id}/godkjenn")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun approveFullmakt(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{id}/avslag")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun rejectFullmakt(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }
}