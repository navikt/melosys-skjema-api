package no.nav.melosys.skjema.integrasjon.pdl

object PdlQuery {
    /**
     * GraphQL query for å hente person med navn og fødselsdato
     */
    const val HENT_PERSON_NAVN_FODSELSDATO = """
        query(${'$'}ident: ID!) {
            hentPerson(ident: ${'$'}ident) {
                navn {
                    fornavn
                    mellomnavn
                    etternavn
                }
                foedselsdato {
                    foedselsdato
                }
            }
        }
    """
}
