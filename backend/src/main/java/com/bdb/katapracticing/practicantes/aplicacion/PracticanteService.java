package com.bdb.katapracticing.practicantes.aplicacion;

import com.bdb.katapracticing.compartido.RecursoNoEncontradoException;
import com.bdb.katapracticing.hojasvida.HojaVidaDescargable;
import com.bdb.katapracticing.hojasvida.HojaVidaGuardada;
import com.bdb.katapracticing.hojasvida.HojasVidaService;
import com.bdb.katapracticing.practicantes.dominio.CorreoDuplicadoException;
import com.bdb.katapracticing.practicantes.dominio.EstadoPracticante;
import com.bdb.katapracticing.practicantes.dominio.Practicante;
import com.bdb.katapracticing.practicantes.persistencia.PracticanteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    public List<Practicante> listar(EstadoPracticante estado) {
        return estado == null
                ? repositorio.findAllByOrderByFechaRegistroDesc()
                : repositorio.findByEstadoOrderByFechaRegistroDesc(estado);
    }

    @Transactional(readOnly = true)
    public Practicante obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el practicante con id " + id));
    }

    @Transactional
    public Practicante actualizarEstado(Long id, EstadoPracticante estado) {
        Practicante practicante = obtener(id);
        practicante.cambiarEstado(estado);
        return practicante; // entidad gestionada: JPA guarda el cambio al cerrar la transacción
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
