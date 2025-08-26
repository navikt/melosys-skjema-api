package no.nav.melosys.skjema.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    @GetMapping("/representasjoner")
    fun getRepresentasjoner(): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }
}