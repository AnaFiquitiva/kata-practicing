package com.bdb.katapracticing.service;

import com.bdb.katapracticing.dto.RegistroPracticanteRequest;
import com.bdb.katapracticing.exception.CorreoDuplicadoException;
import com.bdb.katapracticing.exception.RecursoNoEncontradoException;
import com.bdb.katapracticing.model.EstadoPracticante;
import com.bdb.katapracticing.model.Practicante;
import com.bdb.katapracticing.repository.PracticanteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PracticanteService {

    private final PracticanteRepository repositorio;
    private final AlmacenamientoArchivosService almacenamiento;

    public PracticanteService(PracticanteRepository repositorio, AlmacenamientoArchivosService almacenamiento) {
        this.repositorio = repositorio;
        this.almacenamiento = almacenamiento;
    }

    @Transactional
    public Practicante registrar(RegistroPracticanteRequest datos, MultipartFile hojaVida) {
        String correo = datos.correo().trim().toLowerCase();
        if (repositorio.existsByCorreoIgnoreCase(correo)) {
            throw new CorreoDuplicadoException("Ya existe un registro con este correo electrónico");
        }

        String archivo = almacenamiento.guardarPdf(hojaVida);
        try {
            Practicante p = new Practicante();
            p.setNombreCompleto(datos.nombreCompleto().trim());
            p.setCorreo(correo);
            p.setCarrera(datos.carrera().trim());
            p.setSemestre(datos.semestre());
            p.setHojaVidaNombreOriginal(limpiarNombre(hojaVida.getOriginalFilename()));
            p.setHojaVidaArchivo(archivo);
            return repositorio.saveAndFlush(p);
        } catch (RuntimeException e) {
            almacenamiento.eliminarSilenciosamente(archivo); // no dejar archivos huérfanos
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
        Practicante p = obtener(id);
        p.setEstado(estado);
        p.setFechaActualizacionEstado(LocalDateTime.now());
        return repositorio.save(p);
    }

    private static String limpiarNombre(String original) {
        if (original == null || original.isBlank()) {
            return "hoja-de-vida.pdf";
        }
        // Quita rutas que algunos navegadores envían (C:\fakepath\...) y caracteres de control.
        String nombre = original.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\"]", "");
        return nombre.length() > 150 ? nombre.substring(nombre.length() - 150) : nombre;
    }
}
