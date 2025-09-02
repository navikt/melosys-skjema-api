package no.nav.melosys.skjema.sikkerhet.context

interface SubjectHandler {
    fun getOidcTokenString(): String?
    fun getUserID(): String?
    fun getUserName(): String?
    fun getGroups(): List<String>

    // Ekstra metoder for TokenX-spesifikk informasjon
    fun getAuthenticationLevel(): String? = null
    fun getIdentityProvider(): String? = null
    fun getConsumerInfo(): Pair<String, String>? = null
    fun getOrganizationNumber(): String? = null

    companion object {
        private var subjectHandler: SubjectHandler? = null

        fun getInstance(): SubjectHandler {
            return subjectHandler ?: throw IllegalStateException("SubjectHandler ikke initialisert")
        }

        fun set(handler: SubjectHandler) {
            subjectHandler = handler
        }

        /**
         * Hent brukerens personnummer (fødselsnummer)
         * Dette er primæridentifikatoren for brukere autentisert via ID-porten/TokenX
         */
        fun getSaksbehandlerIdent(): String? {
            return getInstance().getUserID()
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

        /**
         * Hent organisasjonsnummeret hvis det finnes i tokenet
         * Dette ekstraheres fra consumer claim
         */
        fun getOrgNumber(): String? {
            val handler = getInstance()
            return if (handler is SpringSubjectHandler) {
                handler.getOrganizationNumber()
            } else null
        }
    }
}