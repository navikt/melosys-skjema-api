package no.nav.melosys.skjema.sikkerhet.context

interface SubjectHandler {
    fun getOidcTokenString(): String
    fun getUserID(): String
    fun getUserName(): String
    fun getGroups(): List<String>

    // Ekstra metoder for TokenX-spesifikk informasjon
    fun getAuthenticationLevel(): String
    fun getIdentityProvider(): String
    fun getConsumerInfo(): Pair<String, String>

    companion object {
        private var subjectHandler: SubjectHandler? = null

        fun getInstance(): SubjectHandler {
            return subjectHandler ?: throw IllegalStateException("SubjectHandler ikke initialisert")
        }

        fun set(handler: SubjectHandler) {
            subjectHandler = handler
        }

        /**
         * Hent autentiseringsnivået (f.eks. Level3, Level4)
         * Level4 er det høyeste nivået som krever BankID eller tilsvarende
         */
        fun getAuthLevel(): String? {
            val handler = getInstance()
            return if (handler is SpringSubjectHandler) {
                handler.getAuthenticationLevel()
            } else null
        }
    }
}