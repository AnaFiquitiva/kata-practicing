package com.bdb.katapracticing.compartido.web;

import com.bdb.katapracticing.compartido.Rutas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Punto de entrada de la API (HATEOAS). Un cliente solo necesita conocer {@code GET /api}:
 * desde aquí descubre por sus relaciones (rel) todas las demás rutas, sin escribirlas a mano.
 */
@Tag(name = "Raíz", description = "Punto de entrada: enlaces a todos los recursos de la API")
@RestController
public class RaizApiController {

    @Operation(summary = "Punto de entrada de la API (público)")
    @GetMapping(Rutas.API)
    public RepresentationModel<?> raiz() {
        return new RepresentationModel<>()
                .add(enlace(Rutas.API, IanaLinkRelations.SELF, "Punto de entrada", null))
                .add(enlace(Rutas.PRACTICANTES + "{?estado,page,size,sort}", LinkRelation.of("practicantes"),
                        "Listar practicantes (analista)", null))
                .add(enlace(Rutas.PRACTICANTES, LinkRelation.of("registrar-practicante"),
                        "Registrar practicante (POST multipart/form-data)", MediaType.MULTIPART_FORM_DATA_VALUE))
                .add(enlace(Rutas.CSRF, LinkRelation.of("csrf"), "Token CSRF para POST/PATCH", null))
                .add(enlace(Rutas.LOGIN, LinkRelation.of("login"),
                        "Iniciar sesión (POST application/x-www-form-urlencoded)",
                        MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .add(enlace(Rutas.LOGOUT, LinkRelation.of("logout"), "Cerrar sesión (POST)", null))
                .add(enlace(Rutas.USUARIO_ACTUAL, LinkRelation.of("usuario-actual"), "Analista autenticado", null))
                .add(enlace(Rutas.OPENAPI, LinkRelation.of("service-desc"), "Contrato OpenAPI 3", MediaType.APPLICATION_JSON_VALUE))
                .add(enlace(Rutas.SWAGGER_UI, LinkRelation.of("service-doc"), "Documentación Swagger UI", MediaType.TEXT_HTML_VALUE));
    }

    private static Link enlace(String ruta, LinkRelation rel, String titulo, String tipo) {
        String href = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + ruta;
        Link link = Link.of(href, rel).withTitle(titulo);
        return tipo == null ? link : link.withType(tipo);
    }
}
