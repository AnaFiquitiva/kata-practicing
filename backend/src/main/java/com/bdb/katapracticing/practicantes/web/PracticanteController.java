package com.bdb.katapracticing.practicantes.web;

import com.bdb.katapracticing.compartido.Rutas;
import com.bdb.katapracticing.hojasvida.HojaVidaDescargable;
import com.bdb.katapracticing.practicantes.aplicacion.PracticanteService;
import com.bdb.katapracticing.practicantes.dominio.EstadoPracticante;
import com.bdb.katapracticing.practicantes.dominio.Practicante;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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

/** Adaptador HTTP: traduce peticiones REST a casos de uso del servicio. No contiene reglas de negocio. */
@RestController
@RequestMapping(Rutas.PRACTICANTES)
public class PracticanteController {

    private final PracticanteService servicio;

    public PracticanteController(PracticanteService servicio) {
        this.servicio = servicio;
    }

    /** Público: el practicante envía el formulario (multipart/form-data). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PracticanteResponse> registrar(
            @Valid @ModelAttribute RegistroPracticanteRequest datos,
            @RequestPart("hojaVida") MultipartFile hojaVida) {
        Practicante creado = servicio.registrar(datos.comoDatosRegistro(), hojaVida);
        return ResponseEntity
                .created(URI.create(Rutas.PRACTICANTES + "/" + creado.getId()))
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

    /** Analista: descarga la hoja de vida con su nombre original. */
    @GetMapping("/{id}/hoja-vida")
    public ResponseEntity<Resource> descargarHojaVida(@PathVariable Long id) {
        HojaVidaDescargable hojaVida = servicio.obtenerHojaVida(id);
        ContentDisposition adjunto = ContentDisposition.attachment()
                .filename(hojaVida.nombre(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, adjunto.toString())
                .body(hojaVida.contenido());
    }
}
