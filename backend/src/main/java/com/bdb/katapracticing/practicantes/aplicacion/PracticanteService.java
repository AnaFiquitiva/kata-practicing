package com.bdb.katapracticing.practicantes.aplicacion;

import com.bdb.katapracticing.compartido.PrecondicionFallidaException;
import com.bdb.katapracticing.compartido.RecursoNoEncontradoException;
import com.bdb.katapracticing.hojasvida.HojaVidaDescargable;
import com.bdb.katapracticing.hojasvida.HojaVidaGuardada;
import com.bdb.katapracticing.hojasvida.HojasVidaService;
import com.bdb.katapracticing.practicantes.dominio.CorreoDuplicadoException;
import com.bdb.katapracticing.practicantes.dominio.EstadoPracticante;
import com.bdb.katapracticing.practicantes.dominio.Practicante;
import com.bdb.katapracticing.practicantes.persistencia.PracticanteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Casos de uso del proceso de selección: registrar, consultar y decidir sobre un practicante. */
@Service
public class PracticanteService {

    private final PracticanteRepository repositorio;
    private final HojasVidaService hojasVida;

    public PracticanteService(PracticanteRepository repositorio, HojasVidaService hojasVida) {
        this.repositorio = repositorio;
        this.hojasVida = hojasVida;
    }

    /** Datos del formulario ya validados por la capa web (Jakarta Validation). */
    public record DatosRegistro(String nombreCompleto, String correo, String carrera, int semestre) {
    }

    @Transactional
    public Practicante registrar(DatosRegistro datos, MultipartFile archivo) {
        verificarCorreoDisponible(datos.correo());

        HojaVidaGuardada hojaVida = hojasVida.guardar(archivo);
        try {
            Practicante nuevo = Practicante.registrar(
                    datos.nombreCompleto(), datos.correo(), datos.carrera(), datos.semestre(),
                    hojaVida.nombreParaMostrar(), hojaVida.identificador());
            return repositorio.saveAndFlush(nuevo);
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultáneos con el mismo correo: la restricción UNIQUE de la BD decide.
            hojasVida.eliminar(hojaVida.identificador());
            throw new CorreoDuplicadoException();
        } catch (RuntimeException e) {
            hojasVida.eliminar(hojaVida.identificador()); // no dejar archivos huérfanos
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Page<Practicante> listar(EstadoPracticante estado, Pageable pagina) {
        return estado == null
                ? repositorio.findAll(pagina)
                : repositorio.findByEstado(estado, pagina);
    }

    @Transactional(readOnly = true)
    public Practicante obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el practicante con id " + id));
    }

    /**
     * @param versionEsperada versión que el cliente leyó (cabecera {@code If-Match}) o {@code null}. Si viene y ya no es la
     *                        actual, se rechaza con 412 en lugar de pisar el cambio de otro analista.
     */
    @Transactional
    public Practicante actualizarEstado(Long id, EstadoPracticante estado, Long versionEsperada) {
        Practicante practicante = obtener(id);
        if (versionEsperada != null && !practicante.tieneVersion(versionEsperada)) {
            throw new PrecondicionFallidaException();
        }
        practicante.cambiarEstado(estado);
        // flush: así la versión nueva (ETag) ya viene calculada en la respuesta y cualquier
        // conflicto de bloqueo optimista se detecta aquí, dentro de la transacción.
        return repositorio.saveAndFlush(practicante);
    }

    @Transactional(readOnly = true)
    public HojaVidaDescargable obtenerHojaVida(Long id) {
        Practicante practicante = obtener(id);
        return hojasVida.cargar(practicante.getHojaVidaArchivo(), practicante.getHojaVidaNombreOriginal());
    }

    private void verificarCorreoDisponible(String correo) {
        if (repositorio.existsByCorreoIgnoreCase(Practicante.normalizarCorreo(correo))) {
            throw new CorreoDuplicadoException();
        }
    }
}
