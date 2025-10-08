package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.skjema.dto.*
import no.nav.melosys.skjema.service.NotificationService
import no.nav.melosys.skjema.service.SkjemaService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/api/skjema/utsendt-arbeidstaker")
@Tag(name = "Skjema", description = "placeholder")
@Protected
class SkjemaController(
    private val notificationService: NotificationService,
    private val skjemaService: SkjemaService
) {

    @GetMapping
    @Operation(summary = "List skjemaer for current user")
    @ApiResponse(responseCode = "200", description = "List of skjemaer")
    fun listSkjemaer(): ResponseEntity<Any> {
        val skjemaer = skjemaService.listSkjemaerByUser()
        return ResponseEntity.ok(skjemaer)
    }

    @PostMapping("/arbeidsgiver")
    @Operation(summary = "Create new skjema")
    @ApiResponse(responseCode = "201", description = "Skjema created")
    fun createSkjemaArbeidsgiverDel(@RequestBody request: CreateArbeidsgiverSkjemaRequest): ResponseEntity<Any> {
        val skjema = skjemaService.createSkjemaArbeidsgiverDel(request)
        return ResponseEntity.status(201).body(skjema)
    }

    @GetMapping("/arbeidsgiver/{id}")
    @Operation(summary = "Get skjema by ID")
    @ApiResponse(responseCode = "200", description = "Skjema found")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun getSkjemaAsArbeidsgiver(@PathVariable id: UUID): ResponseEntity<Any> {
        val skjema = skjemaService.getSkjemaAsArbeidsgiver(id)
        return ResponseEntity.ok(skjema)
    }

    @GetMapping("/arbeidstaker/{id}")
    @Operation(summary = "Get skjema by ID")
    @ApiResponse(responseCode = "200", description = "Skjema found")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun getSkjemaAsArbeidstaker(@PathVariable id: UUID): ResponseEntity<Any> {
        val skjema = skjemaService.getSkjemaAsArbeidstaker(id)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit skjema")
    @ApiResponse(responseCode = "200", description = "Skjema submitted")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun submitSkjema(@PathVariable id: UUID): ResponseEntity<Any> {
        log.info { "Submitting skjema med id: $id" }

        val skjema = skjemaService.getSkjemaAsArbeidstaker(id)

        try {
            notificationService.sendNotificationToArbeidstaker(id.toString(), "Skjema har blitt sendt til behandling")
            notificationService.sendNotificationToArbeidsgiver("test", "test", "test", skjema.orgnr)
            log.info { "Notifikasjon sendt for skjema med id: $id" }
            return ResponseEntity.ok().build()
        } catch (e: Exception) {
            log.error(e) { "Feil ved sending av notifikasjon for skjema med id: $id" }
            return ResponseEntity.ok().build()
        }
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Generate PDF for skjema")
    @ApiResponse(responseCode = "200", description = "PDF generated")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun generatePdf(@PathVariable id: UUID): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    // Arbeidsgiver Flow Endpoints
    @PostMapping("/arbeidsgiver/{skjemaId}/arbeidsgiveren")
    @Operation(summary = "Register arbeidsgiver information")
    @ApiResponse(responseCode = "200", description = "Arbeidsgiver information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerArbeidsgiver(@PathVariable skjemaId: UUID, @RequestBody request: ArbeidsgiverenDto): ResponseEntity<Any> {
        log.info { "Registering arbeidsgiver: ${request.organisasjonsnummer}" }
        val skjema = skjemaService.saveArbeidsgiverInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidsgiver/{skjemaId}/virksomhet-i-norge")
    @Operation(summary = "Register virksomhet information")
    @ApiResponse(responseCode = "200", description = "Virksomhet information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerVirksomhet(@PathVariable skjemaId: UUID, @RequestBody request: ArbeidsgiverensVirksomhetINorgeDto): ResponseEntity<Any> {
        log.info { "Registering virksomhet information" }
        val skjema = skjemaService.saveVirksomhetInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidsgiver/{skjemaId}/utenlandsoppdraget")
    @Operation(summary = "Register utenlandsoppdrag information")
    @ApiResponse(responseCode = "200", description = "Utenlandsoppdrag information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerUtenlandsoppdrag(@PathVariable skjemaId: UUID, @RequestBody request: UtenlandsoppdragetDto): ResponseEntity<Any> {
        log.info { "Registering utenlandsoppdrag to ${request.utsendelseLand}" }
        val skjema = skjemaService.saveUtenlandsoppdragInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidsgiver/{skjemaId}/arbeidstakerens-lonn")
    @Operation(summary = "Register arbeidstaker lønn information")
    @ApiResponse(responseCode = "200", description = "Arbeidstaker lønn information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerArbeidstakerLonn(@PathVariable skjemaId: UUID, @RequestBody request: ArbeidstakerensLonnDto): ResponseEntity<Any> {
        log.info { "Registering arbeidstaker lønn information" }
        val skjema = skjemaService.saveArbeidstakerLonnInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidsgiver/{skjemaId}/submit")
    @Operation(summary = "Submit arbeidsgiver skjema")
    @ApiResponse(responseCode = "200", description = "Skjema submitted")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun submitArbeidsgiverRequest(@PathVariable skjemaId: UUID, @RequestBody request: SubmitSkjemaRequest): ResponseEntity<Any> {
        log.info { "Submitting arbeidsgiver oppsummering at ${request.submittedAt}" }
        val skjema = skjemaService.submitArbeidsgiver(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    // Arbeidstaker Flow Endpoints
    @PostMapping("/arbeidstaker")
    @Operation(summary = "Create new skjema")
    @ApiResponse(responseCode = "201", description = "Skjema created")
    fun createSkjemaArbeidstakerDel(@RequestBody request: CreateArbeidstakerSkjemaRequest): ResponseEntity<Any> {
        val skjema = skjemaService.createSkjemaArbeidstakerDel(request)
        return ResponseEntity.status(201).body(skjema)
    }

    @PostMapping("/arbeidstaker/{skjemaId}/arbeidstakeren")
    @Operation(summary = "Register arbeidstaker information")
    @ApiResponse(responseCode = "200", description = "Arbeidstaker information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerArbeidstaker(@PathVariable skjemaId: UUID, @RequestBody request: ArbeidstakerenDto): ResponseEntity<Any> {
        val skjema = skjemaService.saveArbeidstakerInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidstaker/{skjemaId}/skatteforhold-og-inntekt")
    @Operation(summary = "Register skatteforhold og inntekt information")
    @ApiResponse(responseCode = "200", description = "Skatteforhold og inntekt information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerSkatteforholdOgInntekt(@PathVariable skjemaId: UUID, @RequestBody request: SkatteforholdOgInntektDto): ResponseEntity<Any> {
        log.info { "Registering skatteforhold og inntekt information" }
        val skjema = skjemaService.saveSkatteforholdOgInntektInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }
    
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleNotFound(): ResponseEntity<Any> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }
}