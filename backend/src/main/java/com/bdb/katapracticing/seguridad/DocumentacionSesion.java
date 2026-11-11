package com.bdb.katapracticing.seguridad;

import com.bdb.katapracticing.compartido.ErrorResponse;
import com.bdb.katapracticing.compartido.Rutas;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Login y logout los atiende Spring Security (no hay controlador), así que springdoc no los ve:
 * aquí se agregan a mano al contrato OpenAPI para que también se puedan probar desde Swagger UI.
 */
@Configuration
class DocumentacionSesion {

    private static final String TAG = "Sesión";
    private static final String CSRF = "csrf";
    private static final String JSON = org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

    @Bean
    OpenApiCustomizer documentarLoginYLogout() {
        return openApi -> {
            Schema<?> error = ModelConverters.getInstance().readAllAsResolvedSchema(ErrorResponse.class).schema;
            openApi.path(Rutas.LOGIN, new PathItem().post(login(error)));
            openApi.path(Rutas.LOGOUT, new PathItem().post(logout()));
        };
    }

    private static Operation login(Schema<?> error) {
        ObjectSchema credenciales = new ObjectSchema();
        credenciales.addProperty("username", new StringSchema().example("analista"));
        credenciales.addProperty("password", new StringSchema().format("password"));
        credenciales.setRequired(List.of("username", "password"));
        Schema<?> usuario = new ObjectSchema().addProperty("usuario", new StringSchema().example("analista"));
        return new Operation()
                .tags(List.of(TAG))
                .operationId("login")
                .summary("Iniciar sesión (analista)")
                .description("Crea la sesión (cookie JSESSIONID). Requiere el token CSRF; después del login pide uno nuevo.")
                .addSecurityItem(new SecurityRequirement().addList(CSRF))
                .requestBody(new RequestBody().required(true).content(new Content().addMediaType(
                        org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                        new MediaType().schema(credenciales))))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().description("Sesión iniciada")
                                .content(new Content().addMediaType(JSON, new MediaType().schema(usuario))))
                        .addApiResponse("401", new ApiResponse().description("Usuario o contraseña incorrectos")
                                .content(new Content().addMediaType(JSON, new MediaType().schema(error))))
                        .addApiResponse("403", new ApiResponse().description("Falta el token CSRF")));
    }

    private static Operation logout() {
        return new Operation()
                .tags(List.of(TAG))
                .operationId("logout")
                .summary("Cerrar sesión")
                .addSecurityItem(new SecurityRequirement().addList(CSRF))
                .responses(new ApiResponses()
                        .addApiResponse("204", new ApiResponse().description("Sesión cerrada"))
                        .addApiResponse("403", new ApiResponse().description("Falta el token CSRF")));
    }
}
