package no.nav.melosys.skjema.controller.admin

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import java.util.UUID
import no.nav.melosys.skjema.service.AdminService
import no.nav.melosys.skjema.sikkerhet.AdminBeskyttet
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * Administrative endepunkter for drift og feilsøking, eksponert mot melosys-console.
 *
 * Alle endepunkter er beskyttet av [AdminBeskyttet] (Azure AD-token + azp_name-allowlist),
 * og dukker opp som egen OpenAPI-gruppe på `/v3/api-docs/admin`.
 */
@RestController
@RequestMapping("/admin")
@AdminBeskyttet
@Tag(name = "Admin", description = "Administrative endepunkter for drift og feilsøking (melosys-console)")
class AdminController(
    private val adminService: AdminService
) {

    @GetMapping("/statistikk")
    @Operation(summary = "Hent aggregert statistikk for skjema og innsendinger")
    @ApiResponse(responseCode = "200", description = "Statistikk hentet")
    fun hentStatistikk(): AdminStatistikkDto {
        log.info { "Admin: Henter statistikk" }
        return adminService.hentStatistikk()
    }

    @GetMapping("/innsendinger/feilede")
    @Operation(summary = "List innsendinger som har feilet Kafka-sending (KAFKA_FEILET)")
    @ApiResponse(responseCode = "200", description = "Feilede innsendinger hentet")
    fun hentFeiledeInnsendinger(): List<InnsendingAdminDto> {
        log.info { "Admin: Henter feilede innsendinger" }
        return adminService.hentFeiledeInnsendinger()
    }

    @GetMapping("/innsendinger/feilede/antall")
    @Operation(summary = "Antall innsendinger med status KAFKA_FEILET")
    @ApiResponse(responseCode = "200", description = "Antall hentet")
    fun hentAntallFeiledeInnsendinger(): AntallDto {
        return AntallDto(adminService.antallFeiledeInnsendinger())
    }

    @GetMapping("/innsendinger/{innsendingId}")
    @Operation(summary = "Hent detaljer for en enkelt innsending")
    @ApiResponse(responseCode = "200", description = "Innsending hentet")
    @ApiResponse(responseCode = "404", description = "Innsending ikke funnet")
    fun hentInnsending(@PathVariable innsendingId: UUID): InnsendingAdminDto {
        log.info { "Admin: Henter innsending $innsendingId" }
        return adminService.hentInnsending(innsendingId)
    }

    @PostMapping("/innsendinger/{innsendingId}/retry")
    @Operation(summary = "Tving ny prosessering (Kafka-sending) av en enkelt innsending")
    @ApiResponse(responseCode = "200", description = "Reprosessering utført")
    @ApiResponse(responseCode = "404", description = "Innsending ikke funnet")
    fun retryInnsending(@PathVariable innsendingId: UUID): InnsendingAdminDto {
        log.info { "Admin: Retry av innsending $innsendingId" }
        return adminService.retryInnsending(innsendingId)
    }

    @PostMapping("/innsendinger/retry-feilede")
    @Operation(summary = "Tving ny prosessering av alle innsendinger med status KAFKA_FEILET")
    @ApiResponse(responseCode = "200", description = "Reprosessering utført")
    fun retryAlleFeilede(): RetryResultatDto {
        log.info { "Admin: Retry av alle feilede innsendinger" }
        return adminService.retryAlleFeilede()
    }
}
