package no.nav.melosys.skjema.controller

import io.getunleash.Unleash
import io.swagger.v3.oas.annotations.Operation
import no.nav.melosys.skjema.featuretoggle.ToggleNavn
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

    /**
     * Samme kontrakt som melosys-api/melosys-web: `?features=a&features=b` → `{a: true, b: false}`.
     * Kun toggles i [ToggleNavn.ALLE] evalueres – ukjente navn filtreres bort, slik at
     * endepunktet ikke kan brukes til å enumerere andre toggles i teamets delte Unleash-instans.
     */
    @GetMapping
    @Operation(summary = "Evaluer feature toggles")
    fun hentFeatureToggles(@RequestParam(required = false) features: List<String>?): Map<String, Boolean> =
        features.orEmpty()
            .distinct()
            .filter { it in ToggleNavn.ALLE }
            .associateWith { unleash.isEnabled(it) }
}
