package no.nav.melosys.skjema.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.skjema.service.skjemadefinisjon.SkjemaDefinisjonService
import no.nav.melosys.skjema.types.SkjemaType
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.skjemadefinisjon.SkjemaDefinisjonDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for skjemadefinisjoner.
 *
 * Eksponerer endepunkter for å hente skjemadefinisjoner som frontend
 * bruker til å rendre skjemaer og oppsummeringer.
 */
@RestController
@RequestMapping("/api/skjema/definisjon")
@Tag(name = "Skjemadefinisjon", description = "Endepunkter for skjemadefinisjoner med versjonering og språkstøtte")
@Protected
class SkjemaDefinisjonController(
    private val skjemaDefinisjonService: SkjemaDefinisjonService
) {

    @GetMapping("/{type}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Hent skjemadefinisjon",
        description = """
            Henter skjemadefinisjon for en gitt skjematype.
            Brukes av frontend for å rendre skjemaer og oppsummeringer.

            Hvis versjon ikke er spesifisert, returneres aktiv versjon.
            Hvis språk ikke er spesifisert, returneres norsk bokmål (nb).
        """
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Skjemadefinisjon funnet"),
        ApiResponse(responseCode = "400", description = "Ukjent skjematype eller versjon", content = [Content()])
    )
    fun hentDefinisjon(
        @Parameter(description = "Skjematype (f.eks. 'A1')", required = true)
        @PathVariable type: SkjemaType,

        @Parameter(description = "Versjon (valgfri - bruker aktiv versjon hvis ikke spesifisert)")
        @RequestParam(required = false) versjon: String?,

        @Parameter(description = "Språkkode (nb, nn, en). Standard: nb")
        @RequestParam(defaultValue = "nb") sprak: String
    ): SkjemaDefinisjonDto {
        val validertSpråk = Språk.fraKode(sprak)
        return skjemaDefinisjonService.hent(type, versjon, validertSpråk)
    }

    @GetMapping("/{type}/versjon", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Hent aktiv versjon for skjematype",
        description = "Returnerer hvilken versjon som er aktiv for en gitt skjematype."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Aktiv versjon funnet"),
        ApiResponse(responseCode = "400", description = "Ukjent skjematype", content = [Content()])
    )
    fun hentAktivVersjon(
        @Parameter(description = "Skjematype (f.eks. 'A1')", required = true)
        @PathVariable type: SkjemaType
    ): AktivVersjonResponse {
        return AktivVersjonResponse(
            type = type,
            aktivVersjon = skjemaDefinisjonService.hentAktivVersjon(type)
        )
    }

    @GetMapping("/typer", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Hent støttede skjematyper",
        description = "Returnerer liste over alle skjematyper som er støttet."
    )
    fun hentStøttedeTyper(): StøttedeTyperResponse {
        return StøttedeTyperResponse(
            typer = skjemaDefinisjonService.hentStøttedeTyper()
        )
    }
}

/**
 * Response for aktiv versjon.
 */
@Schema(description = "Informasjon om aktiv versjon for en skjematype")
data class AktivVersjonResponse(
    @param:Schema(description = "Skjematype", example = "A1")
    val type: SkjemaType,

    @param:Schema(description = "Aktiv versjon", example = "1")
    val aktivVersjon: String
)

/**
 * Response for støttede typer.
 */
@Schema(description = "Liste over støttede skjematyper")
data class StøttedeTyperResponse(
    @param:Schema(description = "Støttede skjematyper", example = "[\"A1\"]")
    val typer: Set<SkjemaType>
)
