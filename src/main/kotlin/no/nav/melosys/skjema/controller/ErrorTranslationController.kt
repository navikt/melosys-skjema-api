package no.nav.melosys.skjema.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.skjema.translations.TRANSLATIONS
import no.nav.melosys.skjema.translations.dto.ErrorMessageTranslations
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/api/error-translation")
@Tag(name = "Error Translation", description = "Endepunkter for feilmeldinger og oversettelser")
class ErrorTranslationController() {

    @Unprotected
    @GetMapping
    @Operation(
        summary = "Hent feiloversettelser",
        description = "Returnerer alle feiloversettelser"
    )
    @ApiResponse(responseCode = "200", description = "Feiloversettelser hentet")
    fun getErrorTranslations(): ResponseEntity<ErrorMessageTranslations> {
        log.info { "Henter feiloversettelser" }
        return ResponseEntity.ok(TRANSLATIONS)
    }

}

