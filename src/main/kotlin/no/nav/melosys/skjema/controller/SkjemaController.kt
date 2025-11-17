package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import no.nav.melosys.skjema.dto.SubmitSkjemaRequest
import no.nav.melosys.skjema.dto.arbeidsgiver.ArbeidsgiversSkjemaDto
import no.nav.melosys.skjema.dto.arbeidsgiver.CreateArbeidsgiverSkjemaRequest
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiveren.ArbeidsgiverenDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidstakeren.ArbeidstakerenDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.dto.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.dto.arbeidstaker.ArbeidstakersSkjemaDto
import no.nav.melosys.skjema.dto.arbeidstaker.CreateArbeidstakerSkjemaRequest
import no.nav.melosys.skjema.dto.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import no.nav.melosys.skjema.dto.arbeidstaker.dineopplysninger.DineOpplysningerDto
import no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.dto.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.dto.arbeidstaker.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelDto
import no.nav.melosys.skjema.dto.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.service.NotificationService
import no.nav.melosys.skjema.service.SkjemaService
import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/api/skjema/utsendt-arbeidstaker")
@Tag(name = "Skjema", description = "placeholder")
@Protected
class SkjemaController(
    private val notificationService: NotificationService,
    private val skjemaService: SkjemaService,
    private val subjectHandler: SubjectHandler
) {

    @GetMapping
    @Operation(summary = "List skjemaer for current user")
    @ApiResponse(responseCode = "200", description = "List of skjemaer")
    fun listSkjemaer(): ResponseEntity<List<ArbeidstakersSkjemaDto>> {
        val skjemaer = skjemaService.listSkjemaerByUser()
        return ResponseEntity.ok(skjemaer)
    }

    @PostMapping("/arbeidsgiver")
    @Operation(summary = "Create new skjema")
    @ApiResponse(responseCode = "201", description = "Skjema created")
    fun createSkjemaArbeidsgiverDel(@RequestBody request: CreateArbeidsgiverSkjemaRequest): ResponseEntity<ArbeidsgiversSkjemaDto> {
        val skjema = skjemaService.createSkjemaArbeidsgiverDel(request)
        return ResponseEntity.status(201).body(skjema)
    }

    @GetMapping("/arbeidsgiver/{id}")
    @Operation(summary = "Get skjema data by ID")
    @ApiResponse(responseCode = "200", description = "Skjema data found")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun getSkjemaAsArbeidsgiver(@PathVariable id: UUID): ResponseEntity<ArbeidsgiversSkjemaDto> {
        val dto = skjemaService.getSkjemaDtoAsArbeidsgiver(id)
        return ResponseEntity.ok(dto)
    }

    @GetMapping("/arbeidstaker/{id}")
    @Operation(summary = "Get skjema data by ID")
    @ApiResponse(responseCode = "200", description = "Skjema data found")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun getSkjemaAsArbeidstaker(@PathVariable id: UUID): ResponseEntity<ArbeidstakersSkjemaDto> {
        val dto = skjemaService.getSkjemaDtoAsArbeidstaker(id)
        return ResponseEntity.ok(dto)
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit skjema")
    @ApiResponse(responseCode = "200", description = "Skjema submitted")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun submitSkjema(@PathVariable id: UUID): ResponseEntity<Any> { //TODO dette brukes kun som ett test endepunkt nå. Kommer sannsynligvis til å fjernes.
        log.info { "Submitting skjema med id: $id" }

        val skjema = skjemaService.getSkjemaAsArbeidstaker(id)

        try {
            notificationService.sendNotificationToArbeidstaker(subjectHandler.getUserID(), "Skjema har blitt sendt til behandling")
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
    fun registerArbeidsgiver(@PathVariable skjemaId: UUID, @RequestBody request: ArbeidsgiverenDto): ResponseEntity<ArbeidsgiversSkjemaDto> {
        log.info { "Registering arbeidsgiver: ${request.organisasjonsnummer}" }
        val skjema = skjemaService.saveArbeidsgiverInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidsgiver/{skjemaId}/arbeidstakeren")
    @Operation(summary = "Register arbeidstaker information")
    @ApiResponse(responseCode = "200", description = "Arbeidstaker information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerArbeidstakerFromArbeidsgiver(@PathVariable skjemaId: UUID, @Valid @RequestBody request: ArbeidstakerenDto): ResponseEntity<ArbeidsgiversSkjemaDto> {
        log.info { "Registering arbeidstaker information from arbeidsgiver" }
        val skjema = skjemaService.saveArbeidstakerInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidsgiver/{skjemaId}/arbeidsgiverens-virksomhet-i-norge")
    @Operation(summary = "Register virksomhet information")
    @ApiResponse(responseCode = "200", description = "Virksomhet information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerVirksomhet(@PathVariable skjemaId: UUID, @RequestBody request: ArbeidsgiverensVirksomhetINorgeDto): ResponseEntity<ArbeidsgiversSkjemaDto> {
        log.info { "Registering virksomhet information" }
        val skjema = skjemaService.saveVirksomhetInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidsgiver/{skjemaId}/utenlandsoppdraget")
    @Operation(summary = "Register utenlandsoppdrag information")
    @ApiResponse(responseCode = "200", description = "Utenlandsoppdrag information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerUtenlandsoppdrag(@PathVariable skjemaId: UUID, @RequestBody request: UtenlandsoppdragetDto): ResponseEntity<ArbeidsgiversSkjemaDto> {
        log.info { "Registering utenlandsoppdrag" }
        val skjema = skjemaService.saveUtenlandsoppdragInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidsgiver/{skjemaId}/arbeidstakerens-lonn")
    @Operation(summary = "Register arbeidstaker lønn information")
    @ApiResponse(responseCode = "200", description = "Arbeidstaker lønn information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerArbeidstakerLonn(@PathVariable skjemaId: UUID, @RequestBody request: ArbeidstakerensLonnDto): ResponseEntity<ArbeidsgiversSkjemaDto> {
        log.info { "Registering arbeidstaker lønn information" }
        val skjema = skjemaService.saveArbeidstakerLonnInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidsgiver/{skjemaId}/arbeidssted-i-utlandet")
    @Operation(summary = "Register arbeidssted i utlandet information")
    @ApiResponse(responseCode = "200", description = "Arbeidssted i utlandet information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerArbeidsstedIUtlandet(@PathVariable skjemaId: UUID, @RequestBody request: ArbeidsstedIUtlandetDto): ResponseEntity<ArbeidsgiversSkjemaDto> {
        log.info { "Registering arbeidssted i utlandet information" }
        val skjema = skjemaService.saveArbeidsstedIUtlandetInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidsgiver/{skjemaId}/tilleggsopplysninger")
    @Operation(summary = "Register tilleggsopplysninger information")
    @ApiResponse(responseCode = "200", description = "Tilleggsopplysninger information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerTilleggsopplysningerAsArbeidsgiver(@PathVariable skjemaId: UUID, @RequestBody request: TilleggsopplysningerDto): ResponseEntity<ArbeidsgiversSkjemaDto> {
        log.info { "Registering tilleggsopplysninger information from arbeidsgiver" }
        val skjema = skjemaService.saveTilleggsopplysningerInfoAsArbeidsgiver(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidsgiver/{skjemaId}/submit")
    @Operation(summary = "Submit arbeidsgiver skjema")
    @ApiResponse(responseCode = "200", description = "Skjema submitted")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun submitArbeidsgiverRequest(@PathVariable skjemaId: UUID, @RequestBody request: SubmitSkjemaRequest): ResponseEntity<ArbeidstakersSkjemaDto> {
        log.info { "Submitting arbeidsgiver oppsummering at ${request.submittedAt}" }
        val skjema = skjemaService.submitArbeidsgiver(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    // Arbeidstaker Flow Endpoints
    @PostMapping("/arbeidstaker")
    @Operation(summary = "Create new skjema")
    @ApiResponse(responseCode = "201", description = "Skjema created")
    fun createSkjemaArbeidstakerDel(@RequestBody request: CreateArbeidstakerSkjemaRequest): ResponseEntity<ArbeidstakersSkjemaDto> {
        val skjema = skjemaService.createSkjemaArbeidstakerDel(request)
        return ResponseEntity.status(201).body(skjema)
    }

    @PostMapping("/arbeidstaker/{skjemaId}/dine-opplysninger")
    @Operation(summary = "Register dine opplysninger")
    @ApiResponse(responseCode = "200", description = "Dine opplysninger registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerDineOpplysninger(@PathVariable skjemaId: UUID, @Valid @RequestBody request: DineOpplysningerDto): ResponseEntity<ArbeidstakersSkjemaDto> {
        val skjema = skjemaService.saveDineOpplysningerInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidstaker/{skjemaId}/utenlandsoppdraget")
    @Operation(summary = "Register utenlandsoppdraget information")
    @ApiResponse(responseCode = "200", description = "Utenlandsoppdraget information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerUtenlandsoppdragetArbeidstaker(@PathVariable skjemaId: UUID, @RequestBody request: UtenlandsoppdragetArbeidstakersDelDto): ResponseEntity<ArbeidstakersSkjemaDto> {
        log.info { "Registering utenlandsoppdraget information for arbeidstaker" }
        val skjema = skjemaService.saveUtenlandsoppdragetInfoAsArbeidstaker(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidstaker/{skjemaId}/arbeidssituasjon")
    @Operation(summary = "Register arbeidssituasjon information")
    @ApiResponse(responseCode = "200", description = "Arbeidssituasjon information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerArbeidssituasjon(@PathVariable skjemaId: UUID, @RequestBody request: ArbeidssituasjonDto): ResponseEntity<ArbeidstakersSkjemaDto> {
        log.info { "Registering arbeidssituasjon information" }
        val skjema = skjemaService.saveArbeidssituasjonInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidstaker/{skjemaId}/skatteforhold-og-inntekt")
    @Operation(summary = "Register skatteforhold og inntekt information")
    @ApiResponse(responseCode = "200", description = "Skatteforhold og inntekt information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerSkatteforholdOgInntekt(@PathVariable skjemaId: UUID, @RequestBody request: SkatteforholdOgInntektDto): ResponseEntity<ArbeidstakersSkjemaDto> {
        log.info { "Registering skatteforhold og inntekt information" }
        val skjema = skjemaService.saveSkatteforholdOgInntektInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidstaker/{skjemaId}/familiemedlemmer")
    @Operation(summary = "Register familiemedlemmer information")
    @ApiResponse(responseCode = "200", description = "Familiemedlemmer information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerFamiliemedlemmer(@PathVariable skjemaId: UUID, @RequestBody request: FamiliemedlemmerDto): ResponseEntity<ArbeidstakersSkjemaDto> {
        log.info { "Registering familiemedlemmer information" }
        val skjema = skjemaService.saveFamiliemedlemmerInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidstaker/{skjemaId}/tilleggsopplysninger")
    @Operation(summary = "Register tilleggsopplysninger information")
    @ApiResponse(responseCode = "200", description = "Tilleggsopplysninger information registered")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerTilleggsopplysninger(@PathVariable skjemaId: UUID, @RequestBody request: TilleggsopplysningerDto): ResponseEntity<ArbeidstakersSkjemaDto> {
        log.info { "Registering tilleggsopplysninger information" }
        val skjema = skjemaService.saveTilleggsopplysningerInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }
    
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleNotFound(): ResponseEntity<Any> {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    }
}