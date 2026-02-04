package no.nav.melosys.skjema.config

import java.lang.reflect.Method
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.stereotype.Component

/**
 * Custom key generator for PDL bulk queries.
 * Sorter identene før caching for å sikre at samme sett av personer gir cache-treff uavhengig av rekkefølge.
 */
@Component("pdlBolkKeyGenerator")
class PdlBolkKeyGenerator : KeyGenerator {
    override fun generate(target: Any, method: Method, vararg params: Any?): Any {
        @Suppress("UNCHECKED_CAST")
        val identer = params[0] as List<String>
        return identer.sorted().joinToString(",")
    }
}
