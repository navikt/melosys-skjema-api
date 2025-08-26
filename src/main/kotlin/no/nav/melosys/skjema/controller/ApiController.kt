package no.nav.melosys.skjema.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1")
class ApiController {

    @GetMapping("/hello")
    fun hello(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "message" to "Hello from Melosys Skjema API!",
            "version" to "1.0.0"
        ))
    }
}