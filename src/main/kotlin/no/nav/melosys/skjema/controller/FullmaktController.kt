package no.nav.melosys.skjema.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/fullmakt")
class FullmaktController {

    @PostMapping
    fun requestFullmakt(@RequestBody request: Any): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}")
    fun getFullmakt(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{id}/godkjenn")
    fun approveFullmakt(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{id}/avslag")
    fun rejectFullmakt(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }
}