package no.nav.melosys.skjema.sikkerhet

import no.nav.security.token.support.core.api.ProtectedWithClaims

/**
 * Annotasjon for admin-beskyttede endepunkter som kalles fra melosys-console.
 * Kombinerer Azure AD token-validering med klient-tilgangsstyring.
 *
 * Validerer at:
 * 1. Token er gyldig Azure AD-token
 * 2. Tokenets azp_name-claim matcher tillatte klienter fra m2m.admin.clients
 *    (typisk `<cluster>:teammelosys:melosys-console`)
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ProtectedWithClaims(issuer = "azure")
annotation class AdminBeskyttet
