package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.skjema.service.NotificationService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/api/skjema")
@Tag(name = "Skjema", description = "placeholder")
@Protected
class SkjemaController(
    private val notificationService: NotificationService
) {

    @GetMapping
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun listSkjemaer(): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @PostMapping
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun createSkjema(@RequestBody skjema: Any): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun getSkjema(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @PutMapping("/{id}")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun updateSkjema(@PathVariable id: String, @RequestBody skjema: Any): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun deleteSkjema(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun submitSkjema(@PathVariable id: String): ResponseEntity<Any> {
        log.info { "Submitting skjema med id: $id" }

        try {
            notificationService.sendNotificationToArbeidstaker(id, "Skjema har blitt sendt til behandling") //TODO finn ut hva som faktisk skal stå her
            notificationService.sendNotificationToArbeidsgiver("test", "test", "test", "222222") //TODO finn ut hva vi skal sende og hvor
            log.info { "Notifikasjon sendt for skjema med id: $id" }
            return ResponseEntity.ok().build()
        } catch (e: Exception) {
            log.error(e) { "Feil ved sending av notifikasjon for skjema med id: $id" }
            return ResponseEntity.ok().build()
        }
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "placeholder", description = "placeholder")
    @ApiResponse(responseCode = "200", description = "placeholder")
    fun generatePdf(@PathVariable id: String): ResponseEntity<Any> {
        return ResponseEntity.ok().build()
    }

    // Arbeidsgiver Flow Endpoints
    @PostMapping("/v1/arbeidsgiver/arbeidsgiveren")
    @Operation(summary = "Register arbeidsgiver information")
    @ApiResponse(responseCode = "200", description = "Arbeidsgiver information registered")
    fun registerArbeidsgiver(@RequestBody request: ArbeidsgiverRequest): ResponseEntity<Any> {
        log.info { "Registering arbeidsgiver: ${request.organisasjonsnummer}" }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/v1/arbeidsgiver/virksomhet-i-norge")
    @Operation(summary = "Register virksomhet information")
    @ApiResponse(responseCode = "200", description = "Virksomhet information registered")
    fun registerVirksomhet(@RequestBody request: VirksomhetRequest): ResponseEntity<Any> {
        log.info { "Registering virksomhet information" }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/v1/arbeidsgiver/utenlandsoppdraget")
    @Operation(summary = "Register utenlandsoppdrag information")
    @ApiResponse(responseCode = "200", description = "Utenlandsoppdrag information registered")
    fun registerUtenlandsoppdrag(@RequestBody request: UtenlandsoppdragRequest): ResponseEntity<Any> {
        log.info { "Registering utenlandsoppdrag to ${request.utsendelseLand}" }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/v1/arbeidsgiver/arbeidstakerens-lonn")
    @Operation(summary = "Register arbeidstaker lønn information")
    @ApiResponse(responseCode = "200", description = "Arbeidstaker lønn information registered")
    fun registerArbeidstakerLonn(@RequestBody request: ArbeidstakerLonnRequest): ResponseEntity<Any> {
        log.info { "Registering arbeidstaker lønn information" }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/v1/arbeidsgiver/oppsummering")
    @Operation(summary = "Submit arbeidsgiver oppsummering")
    @ApiResponse(responseCode = "200", description = "Oppsummering submitted")
    fun submitArbeidsgiverOppsummering(@RequestBody request: OppsummeringRequest): ResponseEntity<Any> {
        log.info { "Submitting arbeidsgiver oppsummering at ${request.submittedAt}" }
        return ResponseEntity.ok().build()
    }

    // Arbeidstaker Flow Endpoints
    @PostMapping("/v1/arbeidstaker/arbeidstakeren")
    @Operation(summary = "Register arbeidstaker information")
    @ApiResponse(responseCode = "200", description = "Arbeidstaker information registered")
    fun registerArbeidstaker(@RequestBody request: ArbeidstakerRequest): ResponseEntity<Any> {
        log.info { "Registering arbeidstaker with fnr: ${request.fodselsnummer?.take(6)}******" }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/v1/arbeidstaker/skatteforhold-og-inntekt")
    @Operation(summary = "Register skatteforhold og inntekt information")
    @ApiResponse(responseCode = "200", description = "Skatteforhold og inntekt information registered")
    fun registerSkatteforholdOgInntekt(@RequestBody request: SkatteforholdOgInntektRequest): ResponseEntity<Any> {
        log.info { "Registering skatteforhold og inntekt information" }
        return ResponseEntity.ok().build()
    }
}

// Data classes for Arbeidsgiver Flow
data class ArbeidsgiverRequest(
    val organisasjonsnummer: String,
    val organisasjonNavn: String
)

data class VirksomhetRequest(
    val erArbeidsgiverenOffentligVirksomhet: Boolean,
    val erArbeidsgiverenBemanningsEllerVikarbyraa: Boolean,
    val opprettholderArbeidsgivereVanligDrift: Boolean
)

data class UtenlandsoppdragRequest(
    val utsendelseLand: String,
    val arbeidstakerUtsendelseFraDato: LocalDate,
    val arbeidstakerUtsendelseTilDato: LocalDate,
    val arbeidsgiverHarOppdragILandet: Boolean,
    val arbeidstakerBleAnsattForUtenlandsoppdraget: Boolean,
    val arbeidstakerForblirAnsattIHelePerioden: Boolean,
    val arbeidstakerErstatterAnnenPerson: Boolean,
    val arbeidstakerVilJobbeForVirksomhetINorgeEtterOppdraget: Boolean?,
    val utenlandsoppholdetsBegrunnelse: String?,
    val ansettelsesforholdBeskrivelse: String?,
    val forrigeArbeidstakerUtsendelseFradato: LocalDate?,
    val forrigeArbeidstakerUtsendelseTilDato: LocalDate?
)

data class ArbeidstakerLonnRequest(
    val arbeidsgiverBetalerAllLonnOgNaturaytelserIUtsendingsperioden: Boolean,
    val virksomheterSomUtbetalerLonnOgNaturalytelser: VirksomheterSomUtbetalerLonnOgNaturalytelser?
)

data class VirksomheterSomUtbetalerLonnOgNaturalytelser(
    val norskeVirksomheter: List<NorskVirksomhet>?,
    val utenlandskeVirksomheter: List<UtenlandskVirksomhet>?
)

data class NorskVirksomhet(
    val organisasjonsnummer: String
)

data class UtenlandskVirksomhet(
    val navn: String,
    val organisasjonsnummer: String,
    val vegnavnOgHusnummer: String,
    val bygning: String?,
    val postkode: String,
    val byStedsnavn: String,
    val region: String,
    val land: String,
    val tilhorerSammeKonsern: Boolean
)

data class OppsummeringRequest(
    val bekreftetRiktighet: Boolean,
    val submittedAt: Instant
)

// Data classes for Arbeidstaker Flow
data class ArbeidstakerRequest(
    val harNorskFodselsnummer: Boolean,
    val fodselsnummer: String?,
    val fornavn: String?,
    val etternavn: String?,
    val fodselsdato: LocalDate?,
    val harVaertEllerSkalVaereILonnetArbeidFoerUtsending: Boolean,
    val aktivitetIMaanedenFoerUtsendingen: String,
    val skalJobbeForFlereVirksomheter: Boolean,
    val norskeVirksomheterArbeidstakerJobberForIutsendelsesPeriode: List<NorskVirksomhet>?,
    val utenlandskeVirksomheterArbeidstakerJobberForIutsendelsesPeriode: List<UtenlandskVirksomhet>?
)

data class SkatteforholdOgInntektRequest(
    val erSkattepliktigTilNorgeIHeleutsendingsperioden: Boolean,
    val mottarPengestotteFraAnnetEosLandEllerSveits: Boolean,
    val landSomUtbetalerPengestotte: String?,
    val pengestotteSomMottasFraAndreLandBelop: String?,
    val pengestotteSomMottasFraAndreLandBeskrivelse: String?
)