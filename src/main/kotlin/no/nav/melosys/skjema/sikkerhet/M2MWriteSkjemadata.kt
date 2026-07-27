package no.nav.melosys.skjema.sikkerhet

import no.nav.security.token.support.core.api.ProtectedWithClaims

/**
 * Annotasjon for M2M-beskyttede endepunkter som gir skrivetilgang til skjemadata.
 * Kombinerer Azure AD token-validering med klient-tilgangsstyring.
 *
 * Valideres mot egen klientliste (m2m.write-skjemadata.clients), adskilt fra
 * lesetilgangen, siden endepunktet muterer data.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ProtectedWithClaims(issuer = "azure")
annotation class M2MWriteSkjemadata
