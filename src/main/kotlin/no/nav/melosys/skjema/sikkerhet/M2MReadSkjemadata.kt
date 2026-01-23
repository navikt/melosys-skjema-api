package no.nav.melosys.skjema.sikkerhet

import no.nav.security.token.support.core.api.ProtectedWithClaims

/**
 * Annotasjon for M2M-beskyttede endepunkter som gir lesetilgang til skjemadata.
 * Kombinerer Azure AD token-validering med klient-tilgangsstyring.
 *
 * Validerer at:
 * 1. Token er gyldig Azure AD-token
 * 2. Tokenets azp-claim matcher tillatte klienter fra m2m.read-skjemadata.clients
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ProtectedWithClaims(issuer = "azure")
annotation class M2MReadSkjemadata
