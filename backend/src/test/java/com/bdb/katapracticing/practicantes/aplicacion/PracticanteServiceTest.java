package com.bdb.katapracticing.practicantes.aplicacion;

import com.bdb.katapracticing.compartido.PrecondicionFallidaException;
import com.bdb.katapracticing.compartido.RecursoNoEncontradoException;
import com.bdb.katapracticing.hojasvida.ArchivoInvalidoException;
import com.bdb.katapracticing.hojasvida.HojaVidaGuardada;
import com.bdb.katapracticing.hojasvida.HojasVidaService;
import com.bdb.katapracticing.practicantes.aplicacion.PracticanteService.DatosRegistro;
import com.bdb.katapracticing.practicantes.dominio.CorreoDuplicadoException;
import com.bdb.katapracticing.practicantes.dominio.EstadoPracticante;
import com.bdb.katapracticing.practicantes.dominio.Practicante;
import com.bdb.katapracticing.practicantes.persistencia.PracticanteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del módulo practicantes: el módulo de hojas de vida se reemplaza por un
 * doble de prueba de su API pública, sin disco ni base de datos.
 */
@ExtendWith(MockitoExtension.class)
class PracticanteServiceTest {

    private static final String ARCHIVO_INTERNO = "uuid.pdf";
    private static final HojaVidaGuardada GUARDADA = new HojaVidaGuardada(ARCHIVO_INTERNO, "cv.pdf");

    @Mock PracticanteRepository repositorio;
    @Mock HojasVidaService hojasVida;

    private PracticanteService servicio;
    private final DatosRegistro datos = new DatosRegistro("Ana Pérez", "Ana@Correo.com", "Sistemas", 8);
    private final MockMultipartFile pdf = new MockMultipartFile("hojaVida", "cv.pdf", "application/pdf", new byte[]{1});

    @BeforeEach
    void configurar() {
        servicio = new PracticanteService(repositorio, hojasVida);
    }

    @Test
    void registrar_guardaArchivoYPracticante() {
        when(hojasVida.guardar(pdf)).thenReturn(GUARDADA);
        when(repositorio.saveAndFlush(any(Practicante.class))).thenAnswer(inv -> inv.getArgument(0));

        Practicante creado = servicio.registrar(datos, pdf);

        assertThat(creado.getCorreo()).isEqualTo("ana@correo.com");
        assertThat(creado.getHojaVidaArchivo()).isEqualTo(ARCHIVO_INTERNO);
        assertThat(creado.getHojaVidaNombreOriginal()).isEqualTo("cv.pdf");
        verify(hojasVida, never()).eliminar(any());
    }

    @Test
    void registrar_correoYaExistente_noGuardaArchivo() {
        when(repositorio.existsByCorreoIgnoreCase("ana@correo.com")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(datos, pdf)).isInstanceOf(CorreoDuplicadoException.class);
        verify(hojasVida, never()).guardar(any());
    }

    @Test
    void registrar_archivoInvalido_noTocaLaBaseDeDatos() {
        when(hojasVida.guardar(pdf)).thenThrow(new ArchivoInvalidoException("El archivo no es un PDF válido"));

        assertThatThrownBy(() -> servicio.registrar(datos, pdf)).isInstanceOf(ArchivoInvalidoException.class);
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void registrar_correoDuplicadoEnCarrera_eliminaArchivoYResponde409() {
        when(hojasVida.guardar(pdf)).thenReturn(GUARDADA);
        when(repositorio.saveAndFlush(any(Practicante.class)))
                .thenThrow(new DataIntegrityViolationException("UNIQUE constraint failed: practicantes.correo"));

        assertThatThrownBy(() -> servicio.registrar(datos, pdf)).isInstanceOf(CorreoDuplicadoException.class);
        verify(hojasVida).eliminar(ARCHIVO_INTERNO);
    }

    @Test
    void registrar_falloInesperadoEnBd_eliminaArchivoYPropagaElError() {
        when(hojasVida.guardar(pdf)).thenReturn(GUARDADA);
        when(repositorio.saveAndFlush(any(Practicante.class))).thenThrow(new IllegalStateException("BD caída"));

        assertThatThrownBy(() -> servicio.registrar(datos, pdf)).isInstanceOf(IllegalStateException.class);
        verify(hojasVida).eliminar(ARCHIVO_INTERNO);
    }

    @Test
    void actualizarEstado_delegaEnLaEntidad() {
        Practicante existente = Practicante.registrar("Ana", "ana@correo.com", "Sistemas", 8, "cv.pdf", ARCHIVO_INTERNO);
        when(repositorio.findById(1L)).thenReturn(Optional.of(existente));
        when(repositorio.saveAndFlush(existente)).thenReturn(existente);

        Practicante actualizado = servicio.actualizarEstado(1L, EstadoPracticante.NO_VIABLE, null);

        assertThat(actualizado.getEstado()).isEqualTo(EstadoPracticante.NO_VIABLE);
        assertThat(actualizado.getFechaActualizacionEstado()).isNotNull();
    }

    @Test
    void actualizarEstado_conVersionVigente_aplicaElCambio() {
        Practicante existente = Practicante.registrar("Ana", "ana@correo.com", "Sistemas", 8, "cv.pdf", ARCHIVO_INTERNO);
        when(repositorio.findById(1L)).thenReturn(Optional.of(existente));
        when(repositorio.saveAndFlush(existente)).thenReturn(existente);

        assertThat(servicio.actualizarEstado(1L, EstadoPracticante.VIABLE, 0L).getEstado())
                .isEqualTo(EstadoPracticante.VIABLE);
    }

    @Test
    void actualizarEstado_conVersionDesactualizada_lanza412YNoCambiaNada() {
        Practicante existente = Practicante.registrar("Ana", "ana@correo.com", "Sistemas", 8, "cv.pdf", ARCHIVO_INTERNO);
        when(repositorio.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> servicio.actualizarEstado(1L, EstadoPracticante.VIABLE, 7L))
                .isInstanceOf(PrecondicionFallidaException.class);
        assertThat(existente.getEstado()).isEqualTo(EstadoPracticante.PENDIENTE);
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void listar_sinEstado_consultaTodosYConEstado_filtra() {
        Pageable pagina = PageRequest.of(0, 20);
        when(repositorio.findAll(pagina)).thenReturn(new PageImpl<>(List.of()));
        when(repositorio.findByEstado(EstadoPracticante.VIABLE, pagina)).thenReturn(new PageImpl<>(List.of()));

        servicio.listar(null, pagina);
        servicio.listar(EstadoPracticante.VIABLE, pagina);

        verify(repositorio).findAll(pagina);
        verify(repositorio).findByEstado(EstadoPracticante.VIABLE, pagina);
    }

    @Test
    void obtenerHojaVida_pideElArchivoInternoConElNombreOriginal() {
        Practicante existente = Practicante.registrar("Ana", "ana@correo.com", "Sistemas", 8, "cv.pdf", ARCHIVO_INTERNO);
        when(repositorio.findById(1L)).thenReturn(Optional.of(existente));

        servicio.obtenerHojaVida(1L);

        verify(hojasVida).cargar(ARCHIVO_INTERNO, "cv.pdf");
    }

    @Test
    void obtener_inexistente_lanzaNoEncontrado() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
