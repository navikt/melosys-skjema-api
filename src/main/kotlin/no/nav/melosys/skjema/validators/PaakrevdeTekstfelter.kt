package no.nav.melosys.skjema.validators

/**
 * Gir [FELT_ER_PAAKREVD] for det første feltet som mangler eller bare inneholder mellomrom.
 *
 * @param felter par av feltnavn og verdi, i den rekkefølgen de skal valideres
 */
fun validerPaakrevdeTekstfelter(vararg felter: Pair<String, String?>): List<Violation> =
    felter.firstOrNull { (_, verdi) -> verdi.isNullOrBlank() }
        ?.let { (felt, _) -> listOf(Violation(field = felt, translationKey = FELT_ER_PAAKREVD)) }
        ?: emptyList()
