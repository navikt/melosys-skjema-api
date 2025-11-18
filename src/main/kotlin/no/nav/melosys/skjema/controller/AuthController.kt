package no.nav.melosys.skjema.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Autentisering og representasjon")
@Protected
class AuthController(
    private val reprService: ReprService
) {

    @GetMapping("/representasjoner")
    @Operation(
        summary = "Hent representasjoner for innlogget bruker",
        description = "Returnerer liste over personer som innlogget bruker kan representere (alias til /api/fullmakt/kan-representere)"
    )
    @ApiResponse(responseCode = "200", description = "Liste over fullmakter")
    @ApiResponse(responseCode = "401", description = "Ikke autentisert")
    fun getRepresentasjoner(): ResponseEntity<List<Fullmakt>> {
        val fullmakter = reprService.hentFullmakterForInnloggetBruker()
        return ResponseEntity.ok(fullmakter)
    }
}