package no.nav.melosys.skjema.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/skjema")
class SkjemaController {

    @GetMapping
    fun listSkjemaer(): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @PostMapping
    fun createSkjema(@RequestBody skjema: Any): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}")
    fun getSkjema(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @PutMapping("/{id}")
    fun updateSkjema(@PathVariable id: String, @RequestBody skjema: Any): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{id}")
    fun deleteSkjema(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{id}/submit")
    fun submitSkjema(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}/pdf")
    fun generatePdf(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }
}