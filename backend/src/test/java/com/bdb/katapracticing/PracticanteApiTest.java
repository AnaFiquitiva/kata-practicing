package com.bdb.katapracticing;

import com.bdb.katapracticing.practicantes.persistencia.PracticanteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/test-practicantes.db",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.uploads.dir=target/test-uploads"
})
class PracticanteApiTest {

    private static final byte[] PDF = "%PDF-1.4\n%prueba\n".getBytes(StandardCharsets.US_ASCII);

    @Autowired MockMvc mvc;
    @Autowired PracticanteRepository repositorio;
    @Autowired ObjectMapper json;

    @BeforeEach
    void limpiar() {
        repositorio.deleteAll();
    }

    private MockMultipartHttpServletRequestBuilder registro(String nombre, String correo, byte[] archivo, String nombreArchivo) {
        return registro(nombre, correo, archivo, nombreArchivo, "8");
    }

    private MockMultipartHttpServletRequestBuilder registro(String nombre, String correo, byte[] archivo,
                                                            String nombreArchivo, String semestre) {
        return (MockMultipartHttpServletRequestBuilder) multipart("/api/practicantes")
                .file(new MockMultipartFile("hojaVida", nombreArchivo, "application/pdf", archivo))
                .param("nombreCompleto", nombre)
                .param("correo", correo)
                .param("carrera", "Ingeniería de Sistemas")
                .param("semestre", semestre)
                .with(csrf());
    }

    private long registrar(String nombre, String correo) throws Exception {
        String respuesta = mvc.perform(registro(nombre, correo, PDF, "cv.pdf"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta).get("id").asLong();
    }

    private static RequestPostProcessor analista() {
        return user("analista").roles("ANALISTA");
    }

    private ResultActions cambiarEstado(long id, String estado, String ifMatch) throws Exception {
        MockHttpServletRequestBuilder peticion = patch("/api/practicantes/{id}/estado", id)
                .with(analista()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"" + estado + "\"}");
        if (ifMatch != null) {
            peticion.header(HttpHeaders.IF_MATCH, ifMatch);
        }
        return mvc.perform(peticion);
    }

    @Test
    void flujoCompleto_registrar_listar_marcarViable_descargar() throws Exception {
        String respuesta = mvc.perform(registro("Ana Pérez", "Ana@Correo.com", PDF, "cv ana.pdf"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/practicantes/")))
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.correo").value("ana@correo.com"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(respuesta).get("id").asLong();

        mvc.perform(get("/api/practicantes").with(analista()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.practicantes.length()").value(1))
                .andExpect(jsonPath("$._embedded.practicantes[0].nombreCompleto").value("Ana Pérez"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        cambiarEstado(id, "VIABLE", "\"0\"")
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))
                .andExpect(jsonPath("$.estado").value("VIABLE"))
                .andExpect(jsonPath("$._links.marcar-viable").doesNotExist())
                .andExpect(jsonPath("$._links.marcar-no-viable.href").exists())
                .andExpect(jsonPath("$._links.marcar-pendiente.href").exists());

        mvc.perform(get("/api/practicantes").param("estado", "VIABLE").with(analista()))
                .andExpect(jsonPath("$._embedded.practicantes.length()").value(1));
        mvc.perform(get("/api/practicantes").param("estado", "NO_VIABLE").with(analista()))
                .andExpect(jsonPath("$._embedded.practicantes.length()").value(0));

        mvc.perform(get("/api/practicantes/{id}/hoja-vida", id).with(user("analista").roles("ANALISTA")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(content().bytes(PDF));
    }

    @Test
    void camposObligatoriosVacios_devuelve400ConErroresPorCampo() throws Exception {
        String respuesta = mvc.perform(registro("   ", "no-es-correo", PDF, "cv.pdf"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        JsonNode errores = json.readTree(respuesta).get("errores");
        assertThat(errores.has("nombreCompleto")).isTrue();
        assertThat(errores.has("correo")).isTrue();
    }

    @Test
    void archivoQueNoEsPdf_esRechazado() throws Exception {
        mvc.perform(registro("Luis", "luis@correo.com", "MZ ejecutable".getBytes(), "cv.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.hojaVida").exists());
    }

    @Test
    void correoDuplicado_devuelve409() throws Exception {
        mvc.perform(registro("Ana", "ana@correo.com", PDF, "cv.pdf")).andExpect(status().isCreated());
        mvc.perform(registro("Ana 2", "ANA@correo.com", PDF, "cv.pdf")).andExpect(status().isConflict());
    }

    @Test
    void sinAutenticacion_noSePuedeListarNiCambiarEstado() throws Exception {
        mvc.perform(get("/api/practicantes")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/practicantes/1/estado").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"VIABLE\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/practicantes/1/hoja-vida")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ANALISTA")
    void estadoInvalido_devuelve400() throws Exception {
        mvc.perform(patch("/api/practicantes/1/estado").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"QUIZAS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_conCredencialesCorrectas_creaSesion() throws Exception {
        MockHttpSession sesion = (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "analista").param("password", "Bogota2026*"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("analista"))
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/api/auth/me").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("analista"));
    }

    @Test
    void login_conClaveIncorrecta_devuelve401() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "analista").param("password", "otra"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void healthCheck_esPublico() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @WithMockUser(roles = "ANALISTA")
    void practicanteInexistente_devuelve404() throws Exception {
        mvc.perform(get("/api/practicantes/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.errores").doesNotExist());
    }

    @Test
    void cambiarEstadoSinTokenCsrf_esRechazado() throws Exception {
        mvc.perform(patch("/api/practicantes/1/estado").with(user("analista").roles("ANALISTA"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"VIABLE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void detalleDeUnPracticante_devuelveSusDatosSinRutaInterna() throws Exception {
        String respuesta = mvc.perform(registro("Sofía Ruiz", "sofia@correo.com", PDF, "cv.pdf"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(respuesta).get("id").asLong();

        mvc.perform(get("/api/practicantes/{id}", id).with(user("analista").roles("ANALISTA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Sofía Ruiz"))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/api/practicantes/" + id)))
                .andExpect(jsonPath("$._links.hoja-vida.href").value(endsWith("/api/practicantes/" + id + "/hoja-vida")))
                .andExpect(jsonPath("$._links.collection.href").exists())
                .andExpect(jsonPath("$._links.marcar-viable.name").value("VIABLE"))
                .andExpect(jsonPath("$._links.marcar-pendiente").doesNotExist())
                .andExpect(jsonPath("$.hojaVidaArchivo").doesNotExist());
    }

    @Test
    void registroSinHojaDeVida_devuelve400EnElCampoHojaVida() throws Exception {
        mvc.perform(multipart("/api/practicantes")
                        .param("nombreCompleto", "Sin Archivo")
                        .param("correo", "sin@correo.com")
                        .param("carrera", "Derecho")
                        .param("semestre", "2")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.hojaVida").value("La hoja de vida (PDF) es obligatoria"));
    }

    @Test
    void tokenCsrf_esPublicoYDiceQueCabeceraUsar() throws Exception {
        mvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
    }

    // ---------------------------------------------------------------- semestre (1 a 10)

    @Test
    void semestreMayorA10_devuelve400EnElCampoSemestre() throws Exception {
        mvc.perform(registro("Leo", "leo@correo.com", PDF, "cv.pdf", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.semestre").value("El semestre debe estar entre 1 y 10"));
    }

    @Test
    void semestre10_esElMaximoPermitido() throws Exception {
        mvc.perform(registro("Leo", "leo@correo.com", PDF, "cv.pdf", "10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.semestre").value(10));
    }

    // ---------------------------------------------------------------- HATEOAS (REST nivel 3)

    @Test
    void raizDeLaApi_esPublicaYEnlazaTodosLosRecursos() throws Exception {
        mvc.perform(get("/api"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.practicantes.templated").value(true))
                .andExpect(jsonPath("$._links.registrar-practicante.href").exists())
                .andExpect(jsonPath("$._links.login.href").exists())
                .andExpect(jsonPath("$._links.csrf.href").exists())
                .andExpect(jsonPath("$._links.service-doc.href").value(endsWith("/api/docs/swagger-ui.html")));
    }

    @Test
    void conHalForms_elPracticanteTraeLaPlantillaParaCambiarEstado() throws Exception {
        long id = registrar("Eva", "eva@correo.com");

        mvc.perform(get("/api/practicantes/{id}", id).with(analista()).accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_FORMS_JSON))
                .andExpect(jsonPath("$._templates.default.method").value("PATCH"))
                .andExpect(jsonPath("$._templates.default.contentType").value("application/json"))
                .andExpect(jsonPath("$._templates.default.properties[0].name").value("estado"))
                .andExpect(jsonPath("$._templates.default.properties[0].options.minItems").value(1))
                .andExpect(jsonPath("$._templates.default.properties[0].readOnly").doesNotExist())
                .andExpect(jsonPath("$._templates.default.properties[0].options.inline.length()").value(3));
    }

    @Test
    void listaVacia_traeEmbeddedVacioYEnlacesDeNavegacion() throws Exception {
        mvc.perform(get("/api/practicantes").with(analista()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.practicantes.length()").value(0))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.filtrar-por-estado.templated").value(true))
                .andExpect(jsonPath("$._links.raiz.href").exists());
    }

    @Test
    void paginacion_ofreceEnlaceNext() throws Exception {
        registrar("Uno", "uno@correo.com");
        registrar("Dos", "dos@correo.com");

        mvc.perform(get("/api/practicantes").param("size", "1").with(analista()))
                .andExpect(jsonPath("$._embedded.practicantes.length()").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(2))
                .andExpect(jsonPath("$._links.next.href").exists());
    }

    @Test
    void ordenarPorCampoNoPermitido_devuelve400() throws Exception {
        mvc.perform(get("/api/practicantes").param("sort", "hojaVidaArchivo").with(analista()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.sort").exists());
    }

    // ---------------------------------------------------------------- integridad de datos

    @Test
    void cambiarAlMismoEstado_devuelve409() throws Exception {
        long id = registrar("Eva", "eva@correo.com");

        cambiarEstado(id, "PENDIENTE", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errores.estado").exists());
    }

    @Test
    void ifMatchDesactualizado_devuelve412YNoPisaElCambio() throws Exception {
        long id = registrar("Eva", "eva@correo.com");
        cambiarEstado(id, "VIABLE", "\"0\"").andExpect(status().isOk()); // otro analista: versión 0 → 1

        cambiarEstado(id, "NO_VIABLE", "\"0\"").andExpect(status().isPreconditionFailed());

        mvc.perform(get("/api/practicantes/{id}", id).with(analista()))
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))
                .andExpect(jsonPath("$.estado").value("VIABLE"));
    }

    @Test
    void ifNoneMatchConLaVersionActual_devuelve304() throws Exception {
        long id = registrar("Eva", "eva@correo.com");

        mvc.perform(get("/api/practicantes/{id}", id).with(analista()).header(HttpHeaders.IF_NONE_MATCH, "\"0\""))
                .andExpect(status().isNotModified());
    }

    @Test
    void patchConTipoDeContenidoNoSoportado_devuelve415() throws Exception {
        mvc.perform(patch("/api/practicantes/1/estado").with(analista()).with(csrf())
                        .contentType(MediaType.TEXT_PLAIN).content("VIABLE"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.mensaje").value("Tipo de contenido no soportado"));
    }

    @Test
    void metodoNoPermitido_devuelve405ConCabeceraAllow() throws Exception {
        mvc.perform(delete("/api/practicantes/1").with(analista()).with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists(HttpHeaders.ALLOW));
    }

    @Test
    void idConFormatoInvalido_devuelve400() throws Exception {
        mvc.perform(get("/api/practicantes/abc").with(analista()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.id").exists());
    }

    // ---------------------------------------------------------------- documentación OpenAPI

    @Test
    void contratoOpenApi_esPublicoYDocumentaElLogin() throws Exception {
        mvc.perform(get("/api/docs/openapi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/practicantes'].post.requestBody.content['multipart/form-data']").exists())
                .andExpect(jsonPath("$.components.schemas.FormularioRegistro.properties.hojaVida.format").value("binary"))
                .andExpect(jsonPath("$.components.schemas.FormularioRegistro.properties.semestre.maximum").value(10))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.operationId").value("login"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists())
                .andExpect(jsonPath("$.components.securitySchemes.csrf.name").value("X-CSRF-TOKEN"));
    }
}
