package no.nav.melosys.skjema.controller

import io.getunleash.Unleash
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/featuretoggle")
@Protected
@Tag(name = "Featuretoggle", description = "Evaluerte feature toggles for frontend")
class FeatureToggleController(
    private val unleash: Unleash
) {

    /** Samme kontrakt som melosys-api/melosys-web: `?features=a&features=b` → `{a: true, b: false}`. */
    @GetMapping
    @Operation(summary = "Evaluer feature toggles")
    fun hentFeatureToggles(@RequestParam features: List<String>): Map<String, Boolean> =
        features.distinct().associateWith { unleash.isEnabled(it) }
}
