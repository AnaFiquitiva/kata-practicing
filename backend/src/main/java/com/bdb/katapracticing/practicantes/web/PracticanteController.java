package com.bdb.katapracticing.practicantes.web;

import com.bdb.katapracticing.compartido.DatoInvalidoException;
import com.bdb.katapracticing.compartido.ETags;
import com.bdb.katapracticing.compartido.ErrorResponse;
import com.bdb.katapracticing.compartido.Rutas;
import com.bdb.katapracticing.hojasvida.HojaVidaDescargable;
import com.bdb.katapracticing.practicantes.aplicacion.PracticanteService;
import com.bdb.katapracticing.practicantes.dominio.EstadoPracticante;
import com.bdb.katapracticing.practicantes.dominio.Practicante;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Adaptador HTTP: traduce peticiones REST a casos de uso del servicio. No contiene reglas de negocio.
 * Responde en HAL ({@code application/hal+json}) o HAL-FORMS ({@code application/prs.hal-forms+json})
 * según la cabecera {@code Accept}: cada respuesta trae los enlaces a las acciones disponibles.
 */
@Tag(name = "Practicantes", description = "Registro del practicante y decisión del analista")
@RestController
@RequestMapping(Rutas.PRACTICANTES)
public class PracticanteController {

    private static final String SESION = "sesion";
    private static final String CSRF = "csrf";
    /** Solo se puede ordenar por estos campos (evita errores y consultas sobre columnas internas). */
    private static final List<String> ORDEN_PERMITIDO = List.of("fechaRegistro", "nombreCompleto", "semestre", "estado");

    private final PracticanteService servicio;
    private final PracticanteModelAssembler ensamblador;
    private final PagedResourcesAssembler<Practicante> paginador;

    public PracticanteController(PracticanteService servicio, PracticanteModelAssembler ensamblador,
                                 PagedResourcesAssembler<Practicante> paginador) {
        this.servicio = servicio;
        this.ensamblador = ensamblador;
        this.paginador = paginador;
    }

    @Operation(summary = "Registrar practicante (público)",
            description = "Formulario multipart con los datos y la hoja de vida en PDF (máx. 5 MB). "
                    + "Semestre entre 1 y 10. Requiere el token CSRF en la cabecera X-CSRF-TOKEN.",
            security = @SecurityRequirement(name = CSRF),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = FormularioRegistroDoc.class))))
    @ApiResponse(responseCode = "201", description = "Creado. La cabecera Location apunta al nuevo recurso")
    @ApiResponse(responseCode = "400", description = "Campos inválidos",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Ya existe un registro con ese correo",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "El PDF supera el tamaño máximo",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EntityModel<PracticanteResponse>> registrar(
            @Parameter(hidden = true) @Valid @ModelAttribute RegistroPracticanteRequest datos,
            @Parameter(hidden = true) @RequestPart("hojaVida") MultipartFile hojaVida) {
        Practicante creado = servicio.registrar(datos.comoDatosRegistro(), hojaVida);
        EntityModel<PracticanteResponse> modelo = ensamblador.toModel(creado);
        return ResponseEntity
                .created(modelo.getRequiredLink("self").toUri())
                .eTag(ETags.de(creado.getVersion()))
                .body(modelo);
    }

    @Operation(summary = "Listar practicantes (analista)",
            description = "Paginado y ordenado por fecha de registro (más recientes primero). "
                    + "Sigue los enlaces next/prev/first/last para recorrer las páginas.",
            security = @SecurityRequirement(name = SESION))
    @ApiResponse(responseCode = "200", description = "Página de practicantes con enlaces de navegación")
    @ApiResponse(responseCode = "400", description = "Parámetro de filtro u orden inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public PagedModel<EntityModel<PracticanteResponse>> listar(
            @Parameter(description = "Filtra por estado") @RequestParam(required = false) EstadoPracticante estado,
            @ParameterObject @PageableDefault(size = 20, sort = "fechaRegistro", direction = Sort.Direction.DESC)
            Pageable pagina) {
        validarOrden(pagina);
        Page<Practicante> resultado = servicio.listar(estado, pagina);
        PagedModel<EntityModel<PracticanteResponse>> modelo = resultado.isEmpty()
                ? emptyModel(resultado)
                : paginador.toModel(resultado, ensamblador);
        modelo.add(linkTo(methodOn(PracticanteController.class).listar(null, null))
                .withRel(RelacionesPracticante.FILTRAR_POR_ESTADO));
        modelo.add(PracticanteModelAssembler.raiz());
        return modelo;
    }

    @SuppressWarnings("unchecked")
    private PagedModel<EntityModel<PracticanteResponse>> emptyModel(Page<Practicante> vacia) {
        // _embedded.practicantes: [] (en vez de omitir _embedded) para que el cliente no tenga casos especiales.
        return (PagedModel<EntityModel<PracticanteResponse>>) (PagedModel<?>)
                paginador.toEmptyModel(vacia, PracticanteResponse.class);
    }

    @Operation(summary = "Consultar un practicante (analista)",
            description = "Devuelve la cabecera ETag con la versión. Con If-None-Match responde 304 si no cambió.",
            security = @SecurityRequirement(name = SESION))
    @ApiResponse(responseCode = "200", description = "Practicante con sus enlaces (acciones disponibles)")
    @ApiResponse(responseCode = "304", description = "No cambió desde la ETag enviada en If-None-Match")
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<PracticanteResponse>> obtener(
            @PathVariable Long id,
            @Parameter(in = ParameterIn.HEADER, description = "ETag que el cliente ya tiene, por ejemplo \"0\"")
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        Practicante practicante = servicio.obtener(id);
        String etag = ETags.de(practicante.getVersion());
        if (ETags.coincide(ifNoneMatch, practicante.getVersion())) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok().eTag(etag).body(ensamblador.toModel(practicante));
    }

    @Operation(summary = "Cambiar el estado de un practicante (analista)",
            description = "Sigue el enlace marcar-viable / marcar-no-viable / marcar-pendiente. "
                    + "Envía If-Match con la ETag leída para no pisar el cambio de otro analista.",
            security = {@SecurityRequirement(name = SESION), @SecurityRequirement(name = CSRF)})
    @ApiResponse(responseCode = "200", description = "Estado actualizado (nueva ETag)")
    @ApiResponse(responseCode = "400", description = "Estado inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Transición no permitida (ya está en ese estado) o edición simultánea",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "412", description = "If-Match no coincide: otro analista lo modificó",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping(path = "/{id}/estado", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EntityModel<PracticanteResponse>> actualizarEstado(
            @PathVariable Long id,
            @Parameter(in = ParameterIn.HEADER, description = "ETag leída, por ejemplo \"0\"", example = "\"0\"")
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody ActualizarEstadoRequest cuerpo) {
        Practicante actualizado = servicio.actualizarEstado(id, cuerpo.getEstado(), ETags.versionDe(ifMatch));
        return ResponseEntity.ok()
                .eTag(ETags.de(actualizado.getVersion()))
                .body(ensamblador.toModel(actualizado));
    }

    @Operation(summary = "Descargar la hoja de vida (analista)", security = @SecurityRequirement(name = SESION))
    @ApiResponse(responseCode = "200", description = "PDF como adjunto con su nombre original",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @ApiResponse(responseCode = "404", description = "No existe el practicante o su archivo",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
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

    /** La página nunca llega en null: Spring la crea con los valores de {@code @PageableDefault}. */
    private static void validarOrden(Pageable pagina) {
        for (Sort.Order orden : pagina.getSort()) {
            if (!ORDEN_PERMITIDO.contains(orden.getProperty())) {
                throw new DatoInvalidoException("sort",
                        "No se puede ordenar por '" + orden.getProperty() + "'. Campos permitidos: " + ORDEN_PERMITIDO);
            }
        }
    }
}
