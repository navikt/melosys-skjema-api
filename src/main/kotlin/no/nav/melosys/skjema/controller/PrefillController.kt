package no.nav.melosys.skjema.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/prefill")
class PrefillController {

    @PostMapping("/person")
    fun getPersonData(@RequestBody request: Any): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @GetMapping("/org/{orgnr}")
    fun getOrgData(@PathVariable orgnr: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }
}