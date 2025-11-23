package no.nav.melosys.skjema.config

import no.nav.melosys.skjema.sikkerhet.context.SubjectHandler
import org.springframework.stereotype.Component

/**
 * Helper for å generere cache-nøkler basert på innlogget bruker.
 * Håndterer tilfeller der SubjectHandler ikke er initialisert (f.eks. i tester).
 */
@Component("cacheKeyProvider")
class CacheKeyProvider {

    fun getUserId(): String? {
        return try {
            SubjectHandler.getInstance().getUserID()
        } catch (e: IllegalStateException) {
            // SubjectHandler ikke initialisert (f.eks. i tester eller før autentisering)
            // Returnerer null for å unngå caching
            null
        }
    }
}
