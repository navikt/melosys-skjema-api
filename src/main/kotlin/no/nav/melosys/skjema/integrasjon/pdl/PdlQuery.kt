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

    /**
     * GraphQL query for å hente flere personer samtidig (bulk)
     * Brukes for å hente personer med fullmakt
     */
    const val HENT_PERSON_BOLK = """
        query(${'$'}identer: [ID!]!) {
            hentPersonBolk(identer: ${'$'}identer) {
                ident
                person {
                    navn {
                        fornavn
                        mellomnavn
                        etternavn
                    }
                    foedselsdato {
                        foedselsdato
                    }
                }
                code
            }
        }
    """
}
