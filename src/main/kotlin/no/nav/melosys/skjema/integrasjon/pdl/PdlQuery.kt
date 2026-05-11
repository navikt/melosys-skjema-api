package no.nav.melosys.skjema.integrasjon.pdl

object PdlQuery {

    private const val NAVN_FELT = """
        fornavn
        mellomnavn
        etternavn
        metadata {
            historisk
            endringer {
                type
                registrert
            }
        }
    """

    private const val FOEDSELSDATO_FELT = """
        foedselsdato
        metadata {
            historisk
            endringer {
                type
                registrert
            }
        }
    """

    const val HENT_PERSON_NAVN_FODSELSDATO = """
        query(${'$'}ident: ID!) {
            hentPerson(ident: ${'$'}ident) {
                navn { $NAVN_FELT }
                foedselsdato { $FOEDSELSDATO_FELT }
            }
        }
    """

    /**
     * Brukes for å hente personer med fullmakt
     */
    const val HENT_PERSON_BOLK = """
        query(${'$'}identer: [ID!]!) {
            hentPersonBolk(identer: ${'$'}identer) {
                ident
                person {
                    navn { $NAVN_FELT }
                    foedselsdato { $FOEDSELSDATO_FELT }
                }
                code
            }
        }
    """
}
