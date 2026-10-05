package no.nav.melosys.skjema.sikkerhet

import no.nav.security.token.support.core.api.ProtectedWithClaims

/**
 * Annotasjon for admin-beskyttede endepunkter som kalles fra melosys-console.
 * Kombinerer Azure AD token-validering med klient-tilgangsstyring.
 *
 * Validerer at:
 * 1. Token er gyldig Azure AD-token
 * 2. Tokenets azp-claim er Consoles klient-ID fra admin.console-klient-id
 *    (se [M2MProtectedAspect.validateAdminAccess])
 *
 * I tillegg krever [no.nav.melosys.skjema.sikkerhet.AdminTilgangInterceptor] driftsgruppen
 * for personkall under /admin.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ProtectedWithClaims(issuer = "azure")
annotation class AdminBeskyttet
