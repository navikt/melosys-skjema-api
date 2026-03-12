package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import no.nav.melosys.skjema.service.HentInnsendteSoknaderUtsendtArbeidstakerSkjemaService
import no.nav.melosys.skjema.service.UtsendtArbeidstakerService
import no.nav.melosys.skjema.types.HentInnsendteSoknaderRequest
import no.nav.melosys.skjema.types.HentUtkastRequest
import no.nav.melosys.skjema.types.InnsendtSkjemaResponse
import no.nav.melosys.skjema.types.InnsendteSoknaderResponse
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OpprettUtsendtArbeidstakerSoknadRequest
import no.nav.melosys.skjema.types.utsendtarbeidstaker.OpprettUtsendtArbeidstakerSoknadResponse
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Representasjonstype
import no.nav.melosys.skjema.types.SkjemaInnsendtKvittering
import no.nav.melosys.skjema.types.UtkastListeResponse
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtenlandsoppdragetDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidssituasjonDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.FamiliemedlemmerDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.types.common.Språk
import no.nav.melosys.skjema.types.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendingsperiodeOgLandDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/api/skjema/utsendt-arbeidstaker")
@Tag(name = "Skjema", description = "placeholder")
@Protected
class UtsendtArbeidstakerController(
    private val utsendtArbeidstakerService: UtsendtArbeidstakerService,
    private val hentInnsendteSoknaderService: HentInnsendteSoknaderUtsendtArbeidstakerSkjemaService,
) {

    @GetMapping
    @Operation(summary = "List skjemaer for current user")
    @ApiResponse(responseCode = "200", description = "List of skjemaer")
    fun listSkjemaer(): ResponseEntity<List<UtsendtArbeidstakerSkjemaDto>> {
        log.info { "Henter alle skjemaer for bruker" }
        return ResponseEntity.ok(utsendtArbeidstakerService.listAlleSkjemaerForBruker())
    }

    @GetMapping("/utkast")
    @Operation(summary = "Hent utkast basert på representasjonskontekst")
    @ApiResponse(responseCode = "200", description = "Liste over utkast hentet")
    @ApiResponse(responseCode = "400", description = "Ugyldig forespørsel")
    fun hentUtkast(
        @RequestParam representasjonstype: Representasjonstype,
        @RequestParam(required = false) radgiverfirmaOrgnr: String?
    ): ResponseEntity<UtkastListeResponse> {
        log.info { "Henter utkast for representasjonstype: $representasjonstype" }

        val request = HentUtkastRequest(
            representasjonstype = representasjonstype,
            radgiverfirmaOrgnr = radgiverfirmaOrgnr
        )

        val response = utsendtArbeidstakerService.hentUtkast(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/innsendte")
    @Operation(summary = "Hent innsendte søknader basert på representasjonskontekst med paginering, søk og sortering")
    @ApiResponse(responseCode = "200", description = "Paginert liste over innsendte søknader hentet")
    @ApiResponse(responseCode = "400", description = "Ugyldig forespørsel")
    fun hentInnsendteSoknader(
        @RequestBody @Valid request: HentInnsendteSoknaderRequest
    ): ResponseEntity<InnsendteSoknaderResponse> {
        log.info { "Henter innsendte søknader for representasjonstype: ${request.representasjonstype}, side: ${request.side}" }
        val response = hentInnsendteSoknaderService.hentInnsendteSoknader(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Hent skjema")
    @ApiResponse(responseCode = "200", description = "Skjema hentet")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang")
    @ApiResponse(responseCode = "404", description = "Skjema ikke funnet")
    fun getSkjema(@PathVariable id: UUID): ResponseEntity<UtsendtArbeidstakerSkjemaDto> {
        log.info { "Henter skjema: $id" }
        return ResponseEntity.ok(utsendtArbeidstakerService.hentSkjema(id))
    }

    @PostMapping("/opprett-med-kontekst")
    @Operation(summary = "Opprett søknad med forhåndsvalgt kontekst")
    @ApiResponse(responseCode = "201", description = "Søknad opprettet")
    @ApiResponse(responseCode = "400", description = "Ugyldig forespørsel eller validering feilet")
    fun opprettUtsendtArbeidstakerSoknad(@RequestBody @Valid request: OpprettUtsendtArbeidstakerSoknadRequest): ResponseEntity<OpprettUtsendtArbeidstakerSoknadResponse> {
        log.info { "Oppretter søknad med kontekst. Type: ${request.representasjonstype}" }
        val response = utsendtArbeidstakerService.opprettUtsendtArbeidstakerSoknad(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }


    @PostMapping("/{id}/send-inn")
    @Operation(summary = "Send inn skjema")
    @ApiResponse(responseCode = "200", description = "Skjema innsendt")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun sendInnSkjema(@PathVariable id: UUID): ResponseEntity<SkjemaInnsendtKvittering> {
        log.info { "Sender inn skjema med id: $id" }
        val innsendtSkjemaKvittering = utsendtArbeidstakerService.sendInnSkjema(id)

        return ResponseEntity.ok(innsendtSkjemaKvittering)
    }

    @GetMapping("/{id}/innsendt-kvittering")
    @Operation(summary = "Hent kvittering for innsendt skjema")
    @ApiResponse(responseCode = "200", description = "Kvittering hentet")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun hentInnsendingKvittering(@PathVariable id: UUID): ResponseEntity<SkjemaInnsendtKvittering> {
        log.info { "Henter innsending-kvittering for skjema: $id" }
        val innsendtSkjemaKvittering = utsendtArbeidstakerService.genererInnsendtKvittering(id)

        return ResponseEntity.ok(innsendtSkjemaKvittering)
    }

    @GetMapping("/{id}/innsendt")
    @Operation(
        summary = "Hent innsendt søknad med skjemadefinisjon",
        description = """
            Henter innsendt søknad med alle data og skjemadefinisjon for visning.
            Skjemadefinisjonen brukes til å vise søknaden med korrekte tekster
            fra innsendingstidspunkt.

            Hvis språk ikke er spesifisert, brukes språket som ble brukt ved innsending.
        """
    )
    @ApiResponse(responseCode = "200", description = "Innsendt søknad hentet")
    @ApiResponse(responseCode = "400", description = "Skjema er ikke innsendt")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun hentInnsendtSkjema(
        @PathVariable id: UUID,
        @Parameter(description = "Språk for visning (valgfritt - bruker innsendtSpråk som default)")
        @RequestParam(required = false) sprak: String?
    ): ResponseEntity<InnsendtSkjemaResponse> {
        log.info { "Henter innsendt søknad med definisjon: id=$id, språk=$sprak" }
        val språkEnum = sprak?.let { Språk.fraKode(it) }
        val innsendtSkjema = utsendtArbeidstakerService.hentInnsendtSkjema(id, språkEnum)

        return ResponseEntity.ok(innsendtSkjema)
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Generate PDF for skjema")
    @ApiResponse(responseCode = "200", description = "PDF generated")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun generatePdf(@PathVariable id: UUID): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    // Arbeidsgiver Flow Endpoints
    @PostMapping("/{skjemaId}/arbeidsgiverens-virksomhet-i-norge")
    @Operation(summary = "Register virksomhet information")
    @ApiResponse(responseCode = "200", description = "Virksomhet information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerVirksomhet(@PathVariable skjemaId: UUID, @RequestBody @Valid request: ArbeidsgiverensVirksomhetINorgeDto): ResponseEntity<UtsendtArbeidstakerSkjemaDto> {
        log.info { "Registering virksomhet information" }
        val skjema = utsendtArbeidstakerService.saveArbeidsgiverensVirksomhetINorge(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/{skjemaId}/utenlandsoppdraget")
    @Operation(summary = "Register utenlandsoppdrag information")
    @ApiResponse(responseCode = "200", description = "Utenlandsoppdrag information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerUtenlandsoppdrag(@PathVariable skjemaId: UUID, @RequestBody @Valid request: UtenlandsoppdragetDto): ResponseEntity<UtsendtArbeidstakerSkjemaDto> {
        log.info { "Registering utenlandsoppdrag" }
        val skjema = utsendtArbeidstakerService.saveUtenlandsoppdraget(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/{skjemaId}/arbeidstakerens-lonn")
    @Operation(summary = "Register arbeidstaker lønn information")
    @ApiResponse(responseCode = "200", description = "Arbeidstaker lønn information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerArbeidstakerLonn(@PathVariable skjemaId: UUID, @RequestBody @Valid request: ArbeidstakerensLonnDto): ResponseEntity<UtsendtArbeidstakerSkjemaDto> {
        log.info { "Registering arbeidstaker lønn information" }
        val skjema = utsendtArbeidstakerService.saveArbeidstakerensLonn(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/{skjemaId}/arbeidssted-i-utlandet")
    @Operation(summary = "Register arbeidssted i utlandet information")
    @ApiResponse(responseCode = "200", description = "Arbeidssted i utlandet information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerArbeidsstedIUtlandet(@PathVariable skjemaId: UUID, @RequestBody @Valid request: ArbeidsstedIUtlandetDto): ResponseEntity<UtsendtArbeidstakerSkjemaDto> {
        log.info { "Registering arbeidssted i utlandet information" }
        val skjema = utsendtArbeidstakerService.saveArbeidsstedIUtlandet(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/{skjemaId}/tilleggsopplysninger")
    @Operation(summary = "Register tilleggsopplysninger")
    @ApiResponse(responseCode = "200", description = "Tilleggsopplysninger registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerTilleggsopplysninger(@PathVariable skjemaId: UUID, @RequestBody @Valid request: TilleggsopplysningerDto): ResponseEntity<UtsendtArbeidstakerSkjemaDto> {
        log.info { "Registering tilleggsopplysninger" }
        val skjema = utsendtArbeidstakerService.saveTilleggsopplysninger(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    // Arbeidstaker Flow Endpoints
    @PostMapping("/{skjemaId}/utsendingsperiode-og-land")
    @Operation(summary = "Register utsendingsperiode og land information")
    @ApiResponse(responseCode = "200", description = "Utsendingsperiode og land information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerUtsendingsperiodeOgLandArbeidstaker(@PathVariable skjemaId: UUID, @RequestBody @Valid request: UtsendingsperiodeOgLandDto): ResponseEntity<UtsendtArbeidstakerSkjemaDto> {
        log.info { "Registering utsendingsperiode og land information for arbeidstaker" }
        val skjema = utsendtArbeidstakerService.saveUtsendingsperiodeOgLand(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/{skjemaId}/arbeidssituasjon")
    @Operation(summary = "Register arbeidssituasjon information")
    @ApiResponse(responseCode = "200", description = "Arbeidssituasjon information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerArbeidssituasjon(@PathVariable skjemaId: UUID, @RequestBody @Valid request: ArbeidssituasjonDto): ResponseEntity<UtsendtArbeidstakerSkjemaDto> {
        log.info { "Registering arbeidssituasjon information" }
        val skjema = utsendtArbeidstakerService.saveArbeidssituasjon(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/{skjemaId}/skatteforhold-og-inntekt")
    @Operation(summary = "Register skatteforhold og inntekt information")
    @ApiResponse(responseCode = "200", description = "Skatteforhold og inntekt information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerSkatteforholdOgInntekt(@PathVariable skjemaId: UUID, @RequestBody @Valid request: SkatteforholdOgInntektDto): ResponseEntity<UtsendtArbeidstakerSkjemaDto> {
        log.info { "Registering skatteforhold og inntekt information" }
        val skjema = utsendtArbeidstakerService.saveSkatteforholdOgInntekt(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/{skjemaId}/familiemedlemmer")
    @Operation(summary = "Register familiemedlemmer information")
    @ApiResponse(responseCode = "200", description = "Familiemedlemmer information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerFamiliemedlemmer(@PathVariable skjemaId: UUID, @RequestBody @Valid request: FamiliemedlemmerDto): ResponseEntity<UtsendtArbeidstakerSkjemaDto> {
        log.info { "Registering familiemedlemmer information" }
        val skjema = utsendtArbeidstakerService.saveFamiliemedlemmer(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

}