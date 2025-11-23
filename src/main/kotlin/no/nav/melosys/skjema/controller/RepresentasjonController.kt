package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.skjema.controller.dto.PersonMedFullmaktDto
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/api/representasjon")
@Tag(name = "Representasjon", description = "Endepunkter for representasjon og fullmakter")
@Protected
class RepresentasjonController(
    private val reprService: ReprService
) {

    @GetMapping
    @Operation(
        summary = "Hent representasjoner for innlogget bruker",
        description = "Henter liste over personer som innlogget bruker kan representere gjennom fullmakt for MED-området (Medlemskap i folketrygden). " +
                "Returnerer fullmakter hvor innlogget bruker er fullmektig."
    )
    @ApiResponse(responseCode = "200", description = "Liste over fullmakter hvor innlogget bruker er fullmektig")
    @ApiResponse(responseCode = "401", description = "Ikke autentisert")
    @ApiResponse(responseCode = "500", description = "Feil ved henting av fullmakter")
    fun hentRepresentasjoner(): ResponseEntity<List<Fullmakt>> {
        val fullmakter = reprService.hentKanRepresentere()
        return ResponseEntity.ok(fullmakter)
    }

    @GetMapping("/personer")
    @Operation(
        summary = "Hent personer bruker har fullmakt fra",
        description = """enter personer som innlogget bruker kan representere gjennom fullmakt for MED-området, beriket 
            med navn og fødselsdato fra PDL. Kun personer som finnes i PDL returneres."""
    )
    @ApiResponse(responseCode = "200", description = "Liste over personer med fullmakt (navn og fødselsdato fra PDL)")
    @ApiResponse(responseCode = "401", description = "Ikke autentisert")
    @ApiResponse(responseCode = "500", description = "Feil ved henting av personer")
    fun hentPersonerMedFullmakt(): ResponseEntity<List<PersonMedFullmaktDto>> {
        val personer = reprService.hentPersonerMedFullmakt()
        return ResponseEntity.ok(personer)
    }
}
