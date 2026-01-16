package no.nav.melosys.skjema.config

import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
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
}