package no.nav.melosys.skjema.exception

import java.util.UUID

/**
 * Saksnummer er immutabelt når det først er satt på en innsending: melosys-api forsøkte å
 * knytte skjemaet til et annet saksnummer enn det som allerede er registrert.
 */
class SaksnummerKonfliktException(skjemaId: UUID, eksisterendeSaksnummer: String, oppgittSaksnummer: String) :
    RuntimeException(
        "Innsending for skjema $skjemaId har allerede saksnummer $eksisterendeSaksnummer " +
            "og kan ikke endres til $oppgittSaksnummer"
    )
