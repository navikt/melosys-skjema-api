package no.nav.melosys.skjema.sikkerhet.context

/**
 * Test implementation av SubjectHandler for bruk i tester.
 * Simulerer en autentisert bruker med standard test-verdier.
 */
class TestSubjectHandler(
    private val userId: String = "12345678911",
    private val userName: String = "Test Bruker",
    private val oidcToken: String = "test-token",
    private val groups: List<String> = emptyList()
) : SubjectHandler {
    
    override fun getOidcTokenString(): String? = oidcToken
    
    override fun getUserID(): String? = userId
    
    override fun getUserName(): String? = userName
    
    override fun getGroups(): List<String> = groups
}