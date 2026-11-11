package com.bdb.katapracticing.compartido.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contrato OpenAPI 3 (Swagger). Describe los dos mecanismos de seguridad de la API:
 * la cookie de sesión del analista y el token CSRF que exigen las escrituras.
 */
@Configuration
class OpenApiConfig {

    static final String SESION = "sesion";
    static final String CSRF = "csrf";

    @Bean
    OpenAPI contratoApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de selección de practicantes")
                        .version("1.0.0"))
                // La guía de uso (CSRF, login, ETag/If-Match) está en el README, no en la portada de Swagger UI.
                .components(new Components()
                        .addSecuritySchemes(SESION, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID")
                                .description("Cookie de sesión que crea POST /api/auth/login"))
                        .addSecuritySchemes(CSRF, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-CSRF-TOKEN")
                                .description("Token de GET /api/csrf; obligatorio en POST/PATCH")));
    }
}
