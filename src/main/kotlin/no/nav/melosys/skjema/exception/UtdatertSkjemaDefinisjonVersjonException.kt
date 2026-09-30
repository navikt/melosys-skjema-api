package no.nav.melosys.skjema.exception

class UtdatertSkjemaDefinisjonVersjonException(
    val klientVersjon: String?,
    val aktivVersjon: String,
    val utkastVersjon: String? = null
) : RuntimeException("Utkastet er oppdatert til en nyere skjemaversjon. Last siden på nytt.") {
    companion object {
        const val ERROR_CODE = "SKJEMA_DEFINISJON_VERSJON_UTDATERT"
    }
}

