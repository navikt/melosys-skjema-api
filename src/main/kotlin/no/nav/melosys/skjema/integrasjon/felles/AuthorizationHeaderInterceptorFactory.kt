package no.nav.melosys.skjema.integrasjon.felles

import org.springframework.http.HttpHeaders
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.stereotype.Component

/**
 * Lager [ClientHttpRequestInterceptor]-instanser som setter Authorization-header på utgående
 * requests for en gitt OAuth2-klient, til bruk i RestClient-konfigurasjonene (`.requestInterceptor(...)`).
 */
@Component
class AuthorizationHeaderInterceptorFactory(
    private val authorizationHeaderProvider: OAuth2AuthorizationHeaderProvider
) {

    /**
     * Bruker brukertoken (on-behalf-of) når en bruker er i kontekst, ellers systemtoken
     * (client credentials), jf. [OAuth2AuthorizationHeaderProvider.authorizationHeader].
     *
     * @param ekstraTokenHeaders ekstra header-navn som skal få samme token som Authorization
     *   (f.eks. PDL sin `Nav-Consumer-Token`).
     */
    fun authorizationInterceptor(clientName: String, ekstraTokenHeaders: List<String> = emptyList()) =
        tokenInterceptor(ekstraTokenHeaders) { authorizationHeaderProvider.authorizationHeader(clientName) }

    /**
     * Bruker alltid systemtoken (client credentials), jf.
     * [OAuth2AuthorizationHeaderProvider.clientCredentialsAuthorizationHeader].
     *
     * @param ekstraTokenHeaders ekstra header-navn som skal få samme token som Authorization
     *   (f.eks. PDL sin `Nav-Consumer-Token`).
     */
    fun clientCredentialsInterceptor(clientName: String, ekstraTokenHeaders: List<String> = emptyList()) =
        tokenInterceptor(ekstraTokenHeaders) { authorizationHeaderProvider.clientCredentialsAuthorizationHeader(clientName) }

    private fun tokenInterceptor(
        ekstraTokenHeaders: List<String>,
        tokenSupplier: () -> String
    ) = ClientHttpRequestInterceptor { request, body, execution ->
        val token = tokenSupplier()
        request.headers.set(HttpHeaders.AUTHORIZATION, token)
        ekstraTokenHeaders.forEach { header -> request.headers.set(header, token) }
        execution.execute(request, body)
    }
}
