package com.bdb.katapracticing.controller;

import com.bdb.katapracticing.dto.ActualizarEstadoRequest;
import com.bdb.katapracticing.dto.PracticanteResponse;
import com.bdb.katapracticing.dto.RegistroPracticanteRequest;
import com.bdb.katapracticing.model.EstadoPracticante;
import com.bdb.katapracticing.model.Practicante;
import com.bdb.katapracticing.service.AlmacenamientoArchivosService;
import com.bdb.katapracticing.service.PracticanteService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/practicantes")
public class PracticanteController {

    private final PracticanteService servicio;
    private final AlmacenamientoArchivosService almacenamiento;

    public PracticanteController(PracticanteService servicio, AlmacenamientoArchivosService almacenamiento) {
        this.servicio = servicio;
        this.almacenamiento = almacenamiento;
    }

    /** Público: el practicante envía el formulario (multipart/form-data). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PracticanteResponse> registrar(
            @Valid @ModelAttribute RegistroPracticanteRequest datos,
            @RequestPart("hojaVida") MultipartFile hojaVida) {
        Practicante creado = servicio.registrar(datos, hojaVida);
        return ResponseEntity
                .created(URI.create("/api/practicantes/" + creado.getId()))
                .body(PracticanteResponse.desde(creado));
    }

    /** Analista: lista de practicantes, opcionalmente filtrada por estado. */
    @GetMapping
    public List<PracticanteResponse> listar(@RequestParam(required = false) EstadoPracticante estado) {
        return servicio.listar(estado).stream().map(PracticanteResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public PracticanteResponse obtener(@PathVariable Long id) {
        return PracticanteResponse.desde(servicio.obtener(id));
    }

    /** Analista: marca al practicante como VIABLE / NO_VIABLE (o lo regresa a PENDIENTE). */
    @PatchMapping("/{id}/estado")
    public PracticanteResponse actualizarEstado(@PathVariable Long id,
                                                @Valid @RequestBody ActualizarEstadoRequest cuerpo) {
        return PracticanteResponse.desde(servicio.actualizarEstado(id, cuerpo.estado()));
    }

    /** Analista: descarga la hoja de vida. */
    @GetMapping("/{id}/hoja-vida")
    public ResponseEntity<Resource> descargarHojaVida(@PathVariable Long id) {
        Practicante p = servicio.obtener(id);
        Resource archivo = almacenamiento.cargar(p.getHojaVidaArchivo());
        ContentDisposition disposicion = ContentDisposition.attachment()
                .filename(p.getHojaVidaNombreOriginal(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .body(archivo);
    }
}
