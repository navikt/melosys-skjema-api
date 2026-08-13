package no.nav.melosys.skjema.service.skjemadefinisjon

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import org.springframework.core.io.ClassPathResource
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

/**
 * Vaktpost mot stille bokmål-fallback: FlersprakligTekst.hent() feiler ikke når et språk
 * mangler i definisjonen — den viser bokmål. Denne testen krever at hver eneste tekstnode
 * i definisjon.json har nb, nn OG en, slik at hull oppdages her og ikke i produksjon.
 */
class DefinisjonKomplettTest : FunSpec({

    val språk = setOf("nb", "nn", "en")

    fun erTekstnode(node: JsonNode): Boolean =
        node.isObject &&
            node.properties().asSequence().any() &&
            node.propertyNames().all { it in språk } &&
            node.properties().all { it.value.isTextual }

    fun finnMangler(node: JsonNode, path: String, mangler: MutableList<String>, antall: IntArray) {
        if (erTekstnode(node)) {
            antall[0]++
            val manglende = språk.filter { node[it] == null || node[it].asString().isBlank() }
            if (manglende.isNotEmpty()) {
                mangler.add("$path mangler ${manglende.joinToString()}")
            }
            return
        }
        when {
            node.isObject -> node.properties().forEach { (navn, barn) ->
                finnMangler(barn, "$path.$navn", mangler, antall)
            }
            node.isArray -> node.forEachIndexed { i, barn ->
                finnMangler(barn, "$path[$i]", mangler, antall)
            }
        }
    }

    test("alle tekstnoder i UTSENDT_ARBEIDSTAKER v1 har nb, nn og en") {
        val jsonMapper = JsonMapper.builder().build()
        val definisjon = ClassPathResource("skjema-definisjoner/UTSENDT_ARBEIDSTAKER/v1/definisjon.json")
            .inputStream.use { jsonMapper.readTree(it) }

        val mangler = mutableListOf<String>()
        val antall = intArrayOf(0)
        finnMangler(definisjon, "", mangler, antall)

        withClue("traverseringen skal finne tekstnodene (fant ${antall[0]})") {
            antall[0] shouldBeGreaterThanOrEqual 186
        }
        mangler.shouldBeEmpty()
    }
})
