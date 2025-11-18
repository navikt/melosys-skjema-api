package no.nav.melosys.skjema.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Pattern
import no.nav.melosys.skjema.integrasjon.repr.ReprService
import no.nav.melosys.skjema.integrasjon.repr.dto.Fullmakt
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/fullmakt")
@Tag(name = "Fullmakt", description = "Endepunkter for å hente og validere fullmakter fra repr-api")
@Protected
@Validated
class FullmaktController(
    private val reprService: ReprService
) {

    @GetMapping("/kan-representere")
    @Operation(
        summary = "Hent hvem innlogget bruker kan representere",
        description = "Henter liste over personer som innlogget bruker kan representere gjennom fullmakt for MED-området (Medlemskap i folketrygden). " +
                "Returnerer fullmakter hvor innlogget bruker er fullmektig."
    )
    @ApiResponse(responseCode = "200", description = "Liste over fullmakter hvor innlogget bruker er fullmektig")
    @ApiResponse(responseCode = "401", description = "Ikke autentisert")
    @ApiResponse(responseCode = "500", description = "Feil ved henting av fullmakter")
    fun hentKanRepresentere(): ResponseEntity<List<Fullmakt>> {
        val fullmakter = reprService.hentKanRepresentere()
        return ResponseEntity.ok(fullmakter)
    }

    @GetMapping("/valider/skriverettigheter/{fnr}")
    @Operation(
        summary = "Valider skriverettigheter for medlemskap. BRUKES KUN FOR TESTING. BRUKES KUN FOR TESTING. SLETTES ETTERHVERT, MEN SERVICEMETODER VIL GJENSTÅ.",
        description = "Validerer at innlogget bruker har fullmakt med skriverettigheter for MED (Medlemskap) fra gitt fødselsnummer. " +
                "Skriverettigheter betyr at innlogget bruker kan søke på ytelser, klage på vedtak, og sende/endre dokumentasjon på vegne av fullmaktsgiver."
    )
    @ApiResponse(responseCode = "200", description = "true hvis innlogget bruker har fullmakt med skriverettigheter fra gitt fnr, false ellers")
    @ApiResponse(responseCode = "400", description = "Ugyldig fødselsnummer")
    @ApiResponse(responseCode = "401", description = "Ikke autentisert")
    fun validerSkriverettigheter(
        @PathVariable
        @Pattern(regexp = "^\\d{11}$", message = "Fødselsnummer må være 11 siffer")
        fnr: String
    ): ResponseEntity<Boolean> {
        val harRettigheter = reprService.harSkriverettigheterForMedlemskap(fnr)
        return ResponseEntity.ok(harRettigheter)
    }

    @GetMapping("/valider/leserettigheter/{fnr}")
    @Operation(
        summary = "Valider leserettigheter for medlemskap. BRUKES KUN FOR TESTING. SLETTES ETTERHVERT, MEN SERVICEMETODER VIL GJENSTÅ.",
        description = "Validerer at innlogget bruker har fullmakt med leserettigheter for MED (Medlemskap) fra gitt fødselsnummer. " +
                "Leserettigheter betyr at innlogget bruker kan se dokumenter, saker og meldinger, samt snakke med Nav på vegne av fullmaktsgiver."
    )
    @ApiResponse(responseCode = "200", description = "true hvis innlogget bruker har fullmakt med leserettigheter fra gitt fnr, false ellers")
    @ApiResponse(responseCode = "400", description = "Ugyldig fødselsnummer")
    @ApiResponse(responseCode = "401", description = "Ikke autentisert")
    fun validerLeserettigheter(
        @PathVariable
        @Pattern(regexp = "^\\d{11}$", message = "Fødselsnummer må være 11 siffer")
        fnr: String
    ): ResponseEntity<Boolean> {
        val harRettigheter = reprService.harLeserettigheterForMedlemskap(fnr)
        return ResponseEntity.ok(harRettigheter)
    }
}