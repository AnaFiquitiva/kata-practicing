package com.bdb.katapracticing;

import com.bdb.katapracticing.practicantes.persistencia.PracticanteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
        return (MockMultipartHttpServletRequestBuilder) multipart("/api/practicantes")
                .file(new MockMultipartFile("hojaVida", nombreArchivo, "application/pdf", archivo))
                .param("nombreCompleto", nombre)
                .param("correo", correo)
                .param("carrera", "Ingeniería de Sistemas")
                .param("semestre", "8")
                .with(csrf());
    }

    @Test
    void flujoCompleto_registrar_listar_marcarViable_descargar() throws Exception {
        String respuesta = mvc.perform(registro("Ana Pérez", "Ana@Correo.com", PDF, "cv ana.pdf"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.correo").value("ana@correo.com"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(respuesta).get("id").asLong();

        mvc.perform(get("/api/practicantes").with(user("analista").roles("ANALISTA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombreCompleto").value("Ana Pérez"));

        mvc.perform(patch("/api/practicantes/{id}/estado", id)
                        .with(user("analista").roles("ANALISTA")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"VIABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VIABLE"));

        mvc.perform(get("/api/practicantes").param("estado", "VIABLE").with(user("analista").roles("ANALISTA")))
                .andExpect(jsonPath("$.length()").value(1));

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
                .andExpect(jsonPath("$.hojaVidaUrl").value("/api/practicantes/" + id + "/hoja-vida"))
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
}
