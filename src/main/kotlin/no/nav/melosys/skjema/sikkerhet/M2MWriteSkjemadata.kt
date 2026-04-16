package no.nav.melosys.skjema.sikkerhet

import no.nav.security.token.support.core.api.ProtectedWithClaims

/**
 * Annotasjon for M2M-beskyttede endepunkter som gir skrivetilgang til skjemadata.
 * Kombinerer Azure AD token-validering med klient-tilgangsstyring.
 *
 * Bruker samme klientliste som lesetilgang (m2m.read-skjemadata.clients),
 * men er skilt ut som egen annotasjon for å tydeliggjøre at endepunktet muterer data.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ProtectedWithClaims(issuer = "azure")
annotation class M2MWriteSkjemadata
