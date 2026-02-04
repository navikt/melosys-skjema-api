package no.nav.melosys.skjema.config

import java.lang.reflect.Type
import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.JavaType
import org.hibernate.type.format.FormatMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

/**
 * Egen FormatMapper som kobler Hibernate 7 med Jackson 3.
 *
 * Hibernate 7 sin innebygde JacksonJsonFormatMapper bruker Jackson 2 sin ObjectMapper,
 * men Spring Boot 4 bruker Jackson 3. Denne mapperen gir Jackson 3-støtte
 * for JSON-serialisering/-deserialisering i Hibernate-entiteter.
 *
 * NB: Denne klassen blir sannsynligvis overflødig når Hibernate kommer med
 * en release som har innebygd støtte for Jackson 3.
 */
class Jackson3FormatMapper : FormatMapper {

    private val jsonMapper: JsonMapper = JsonMapper.builder()
        .addModule(kotlinModule())
        .build()

    override fun <T : Any?> fromString(
        value: CharSequence,
        javaType: JavaType<T>,
        wrapperOptions: WrapperOptions
    ): T {
        val type: Type = javaType.javaType
        return jsonMapper.readValue(value.toString(), jsonMapper.constructType(type))
    }

    override fun <T : Any?> toString(
        value: T,
        javaType: JavaType<T>,
        wrapperOptions: WrapperOptions
    ): String {
        return jsonMapper.writeValueAsString(value)
    }
}
