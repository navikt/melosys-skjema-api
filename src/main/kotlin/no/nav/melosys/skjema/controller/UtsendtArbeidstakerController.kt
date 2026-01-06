package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import no.nav.melosys.skjema.dto.OpprettSoknadMedKontekstRequest
import no.nav.melosys.skjema.dto.OpprettSoknadMedKontekstResponse
import no.nav.melosys.skjema.dto.SkjemaInnsendtResponse
import no.nav.melosys.skjema.dto.arbeidsgiver.ArbeidsgiversSkjemaDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsgiversvirksomhetinorge.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidsstedIutlandet.ArbeidsstedIUtlandetDto
import no.nav.melosys.skjema.dto.arbeidsgiver.arbeidstakerenslonn.ArbeidstakerensLonnDto
import no.nav.melosys.skjema.dto.arbeidsgiver.utenlandsoppdraget.UtenlandsoppdragetDto
import no.nav.melosys.skjema.dto.arbeidstaker.ArbeidstakersSkjemaDto
import no.nav.melosys.skjema.dto.arbeidstaker.arbeidssituasjon.ArbeidssituasjonDto
import no.nav.melosys.skjema.dto.arbeidstaker.familiemedlemmer.FamiliemedlemmerDto
import no.nav.melosys.skjema.dto.arbeidstaker.skatteforholdoginntekt.SkatteforholdOgInntektDto
import no.nav.melosys.skjema.dto.arbeidstaker.utenlandsoppdraget.UtenlandsoppdragetArbeidstakersDelDto
import no.nav.melosys.skjema.dto.felles.TilleggsopplysningerDto
import no.nav.melosys.skjema.service.HentInnsendteSoknaderUtsendtArbeidstakerSkjemaService
import no.nav.melosys.skjema.service.UtsendtArbeidstakerService
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
    fun listSkjemaer(): ResponseEntity<List<ArbeidstakersSkjemaDto>> {
        log.info { "Henter alle skjemaer for bruker" }
        return ResponseEntity.ok(utsendtArbeidstakerService.listAlleSkjemaerForBruker())
    }

    @GetMapping("/utkast")
    @Operation(summary = "Hent utkast basert på representasjonskontekst")
    @ApiResponse(responseCode = "200", description = "Liste over utkast hentet")
    @ApiResponse(responseCode = "400", description = "Ugyldig forespørsel")
    fun hentUtkast(
        @RequestParam representasjonstype: no.nav.melosys.skjema.dto.Representasjonstype,
        @RequestParam(required = false) radgiverfirmaOrgnr: String?
    ): ResponseEntity<no.nav.melosys.skjema.dto.UtkastListeResponse> {
        log.info { "Henter utkast for representasjonstype: $representasjonstype" }

        val request = no.nav.melosys.skjema.dto.HentUtkastRequest(
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
        @RequestBody @Valid request: no.nav.melosys.skjema.dto.HentInnsendteSoknaderRequest
    ): ResponseEntity<no.nav.melosys.skjema.dto.InnsendteSoknaderResponse> {
        log.info { "Henter innsendte søknader for representasjonstype: ${request.representasjonstype}, side: ${request.side}" }
        val response = hentInnsendteSoknaderService.hentInnsendteSoknader(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}/arbeidsgiver-view")
    @Operation(summary = "Hent skjema med arbeidsgiver-visning")
    @ApiResponse(responseCode = "200", description = "Skjema hentet")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang til arbeidsgiver-visning")
    @ApiResponse(responseCode = "404", description = "Skjema ikke funnet")
    fun getArbeidsgiverView(@PathVariable id: UUID): ResponseEntity<ArbeidsgiversSkjemaDto> {
        log.info { "Henter arbeidsgiver-view for skjema: $id" }
        return ResponseEntity.ok(utsendtArbeidstakerService.getSkjemaArbeidsgiversDel(id))
    }

    @GetMapping("/{id}/arbeidstaker-view")
    @Operation(summary = "Hent skjema med arbeidstaker-visning")
    @ApiResponse(responseCode = "200", description = "Skjema hentet")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang til arbeidstaker-visning")
    @ApiResponse(responseCode = "404", description = "Skjema ikke funnet")
    fun getArbeidstakerView(@PathVariable id: UUID): ResponseEntity<ArbeidstakersSkjemaDto> {
        log.info { "Henter arbeidstaker-view for skjema: $id" }
        return ResponseEntity.ok(utsendtArbeidstakerService.getSkjemaArbeidstakersDel(id))
    }

    @PostMapping("/opprett-med-kontekst")
    @Operation(summary = "Opprett søknad med forhåndsvalgt kontekst")
    @ApiResponse(responseCode = "201", description = "Søknad opprettet")
    @ApiResponse(responseCode = "400", description = "Ugyldig forespørsel eller validering feilet")
    fun opprettSoknadMedKontekst(@RequestBody @Valid request: OpprettSoknadMedKontekstRequest): ResponseEntity<OpprettSoknadMedKontekstResponse> {
        log.info { "Oppretter søknad med kontekst. Type: ${request.representasjonstype}" }
        val response = utsendtArbeidstakerService.opprettMedKontekst(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }


    @PostMapping("/{id}/send-inn")
    @Operation(summary = "Send inn skjema")
    @ApiResponse(responseCode = "200", description = "Skjema innsendt")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun sendInnSkjema(@PathVariable id: UUID): ResponseEntity<SkjemaInnsendtResponse> {
        log.info { "Sender inn skjema med id: $id" }
        val innsendtSkjemaBekreftelse = utsendtArbeidstakerService.sendInnSkjema(id)

        return ResponseEntity.ok(innsendtSkjemaBekreftelse)
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Generate PDF for skjema")
    @ApiResponse(responseCode = "200", description = "PDF generated")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun generatePdf(@PathVariable id: UUID): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    // Arbeidsgiver Flow Endpoints
    @PostMapping("/arbeidsgiver/{skjemaId}/arbeidsgiverens-virksomhet-i-norge")
    @Operation(summary = "Register virksomhet information")
    @ApiResponse(responseCode = "200", description = "Virksomhet information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang til arbeidsgiver-del")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerVirksomhet(@PathVariable skjemaId: UUID, @Valid @RequestBody request: ArbeidsgiverensVirksomhetINorgeDto): ResponseEntity<ArbeidsgiversSkjemaDto> {
        log.info { "Registering virksomhet information" }
        val skjema = utsendtArbeidstakerService.saveVirksomhetInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidsgiver/{skjemaId}/utenlandsoppdraget")
    @Operation(summary = "Register utenlandsoppdrag information")
    @ApiResponse(responseCode = "200", description = "Utenlandsoppdrag information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang til arbeidsgiver-del")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerUtenlandsoppdrag(@PathVariable skjemaId: UUID, @Valid @RequestBody request: UtenlandsoppdragetDto): ResponseEntity<ArbeidsgiversSkjemaDto> {
        log.info { "Registering utenlandsoppdrag" }
        val skjema = utsendtArbeidstakerService.saveUtenlandsoppdragInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidsgiver/{skjemaId}/arbeidstakerens-lonn")
    @Operation(summary = "Register arbeidstaker lønn information")
    @ApiResponse(responseCode = "200", description = "Arbeidstaker lønn information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang til arbeidsgiver-del")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerArbeidstakerLonn(@PathVariable skjemaId: UUID, @Valid @RequestBody request: ArbeidstakerensLonnDto): ResponseEntity<ArbeidsgiversSkjemaDto> {
        log.info { "Registering arbeidstaker lønn information" }
        val skjema = utsendtArbeidstakerService.saveArbeidstakerLonnInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidsgiver/{skjemaId}/arbeidssted-i-utlandet")
    @Operation(summary = "Register arbeidssted i utlandet information")
    @ApiResponse(responseCode = "200", description = "Arbeidssted i utlandet information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang til arbeidsgiver-del")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerArbeidsstedIUtlandet(@PathVariable skjemaId: UUID, @Valid @RequestBody request: ArbeidsstedIUtlandetDto): ResponseEntity<ArbeidsgiversSkjemaDto> {
        log.info { "Registering arbeidssted i utlandet information" }
        val skjema = utsendtArbeidstakerService.saveArbeidsstedIUtlandetInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidsgiver/{skjemaId}/tilleggsopplysninger")
    @Operation(summary = "Register tilleggsopplysninger information")
    @ApiResponse(responseCode = "200", description = "Tilleggsopplysninger information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang til arbeidsgiver-del")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerTilleggsopplysningerAsArbeidsgiver(@PathVariable skjemaId: UUID, @Valid @RequestBody request: TilleggsopplysningerDto): ResponseEntity<ArbeidsgiversSkjemaDto> {
        log.info { "Registering tilleggsopplysninger information from arbeidsgiver" }
        val skjema = utsendtArbeidstakerService.saveTilleggsopplysningerInfoAsArbeidsgiver(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    // Arbeidstaker Flow Endpoints
    @PostMapping("/arbeidstaker/{skjemaId}/utenlandsoppdraget")
    @Operation(summary = "Register utenlandsoppdraget information")
    @ApiResponse(responseCode = "200", description = "Utenlandsoppdraget information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang til arbeidstaker-del")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerUtenlandsoppdragetArbeidstaker(@PathVariable skjemaId: UUID, @Valid @RequestBody request: UtenlandsoppdragetArbeidstakersDelDto): ResponseEntity<ArbeidstakersSkjemaDto> {
        log.info { "Registering utenlandsoppdraget information for arbeidstaker" }
        val skjema = utsendtArbeidstakerService.saveUtenlandsoppdragetInfoAsArbeidstaker(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidstaker/{skjemaId}/arbeidssituasjon")
    @Operation(summary = "Register arbeidssituasjon information")
    @ApiResponse(responseCode = "200", description = "Arbeidssituasjon information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang til arbeidstaker-del")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerArbeidssituasjon(@PathVariable skjemaId: UUID, @Valid @RequestBody request: ArbeidssituasjonDto): ResponseEntity<ArbeidstakersSkjemaDto> {
        log.info { "Registering arbeidssituasjon information" }
        val skjema = utsendtArbeidstakerService.saveArbeidssituasjonInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidstaker/{skjemaId}/skatteforhold-og-inntekt")
    @Operation(summary = "Register skatteforhold og inntekt information")
    @ApiResponse(responseCode = "200", description = "Skatteforhold og inntekt information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang til arbeidstaker-del")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerSkatteforholdOgInntekt(@PathVariable skjemaId: UUID, @Valid @RequestBody request: SkatteforholdOgInntektDto): ResponseEntity<ArbeidstakersSkjemaDto> {
        log.info { "Registering skatteforhold og inntekt information" }
        val skjema = utsendtArbeidstakerService.saveSkatteforholdOgInntektInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidstaker/{skjemaId}/familiemedlemmer")
    @Operation(summary = "Register familiemedlemmer information")
    @ApiResponse(responseCode = "200", description = "Familiemedlemmer information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang til arbeidstaker-del")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerFamiliemedlemmer(@PathVariable skjemaId: UUID, @Valid @RequestBody request: FamiliemedlemmerDto): ResponseEntity<ArbeidstakersSkjemaDto> {
        log.info { "Registering familiemedlemmer information" }
        val skjema = utsendtArbeidstakerService.saveFamiliemedlemmerInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

    @PostMapping("/arbeidstaker/{skjemaId}/tilleggsopplysninger")
    @Operation(summary = "Register tilleggsopplysninger information")
    @ApiResponse(responseCode = "200", description = "Tilleggsopplysninger information registered")
    @ApiResponse(responseCode = "403", description = "Ingen tilgang til arbeidstaker-del")
    @ApiResponse(responseCode = "404", description = "Skjema not found")
    fun registerTilleggsopplysninger(@PathVariable skjemaId: UUID, @Valid @RequestBody request: TilleggsopplysningerDto): ResponseEntity<ArbeidstakersSkjemaDto> {
        log.info { "Registering tilleggsopplysninger information" }
        val skjema = utsendtArbeidstakerService.saveTilleggsopplysningerInfo(skjemaId, request)
        return ResponseEntity.ok(skjema)
    }

}