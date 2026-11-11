package com.bdb.katapracticing;

import com.bdb.katapracticing.repository.PracticanteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;

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
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().bytes(PDF));
    }

    @Test
    void camposObligatoriosVacios_devuelve400ConErroresPorCampo() throws Exception {
        String respuesta = mvc.perform(registro("   ", "no-es-correo", PDF, "cv.pdf"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        JsonNode errores = json.readTree(respuesta).get("errores");
        assert errores.has("nombreCompleto") && errores.has("correo");
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
        var sesion = mvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "analista").param("password", "Bogota2026*"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("analista"))
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/api/auth/me").session((org.springframework.mock.web.MockHttpSession) sesion))
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
}
