package no.nav.melosys.skjema.config

import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    init {
        ModelResolver.enumsAsRef = true
    }

    @Bean
    fun customOpenAPI(): OpenAPI = OpenAPI()
        .info(Info().title("Melosys Skjema Api"))
        .components(
            Components().addSecuritySchemes(
                "bearer-jwt",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .`in`(SecurityScheme.In.HEADER)
                    .name("Authorization")
                    .description("Bearer token"),
            ),
        ).addSecurityItem(SecurityRequirement().addList("bearer-jwt"))

    /**
     * Fjerner "null" fra OpenAPI 3.1 `type`-arrays (f.eks. `["string", "null"]` -> `["string"]`).
     *
     * Springdoc utleder nullability fra Kotlin sin nullable-markering (`String?`),
     * men i kombinasjon med Jackson `default-property-inclusion: non_null` serialiseres
     * aldri null over nettet. Vi vil dermed at OpenAPI-spec'en skal reflektere
     * faktisk wire-kontrakt (felt er fraværende, ikke null), slik at genererte
     * TypeScript-typer blir `T | undefined` i stedet for `T | null | undefined`.
     */
    @Bean
    fun stripNullFromSchemaTypes(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        openApi.components?.schemas?.values?.forEach { stripNullFromSchema(it) }
    }

    private fun stripNullFromSchema(schema: Schema<*>) {
        schema.types?.remove("null")
        if (schema.types?.isEmpty() == true) {
            schema.types = null
        }
        // Springdoc genererer `oneOf: [{$ref}, {types: ["null"]}]` for nullable
        // $ref-felter. Vi vil bare beholde $ref-grenen.
        schema.oneOf = schema.oneOf?.filterNot { it.types?.singleOrNull() == "null" }

        schema.properties?.values?.forEach { stripNullFromSchema(it) }
        schema.items?.let { stripNullFromSchema(it) }
        schema.additionalProperties?.let {
            if (it is Schema<*>) stripNullFromSchema(it)
        }
        schema.allOf?.forEach { stripNullFromSchema(it) }
        schema.anyOf?.forEach { stripNullFromSchema(it) }
        schema.oneOf?.forEach { stripNullFromSchema(it) }
    }
}