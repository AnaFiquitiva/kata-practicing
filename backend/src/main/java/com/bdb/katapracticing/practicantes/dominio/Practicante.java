package com.bdb.katapracticing.practicantes.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * Practicante que se postula. La entidad protege sus propias reglas:
 * nace siempre en PENDIENTE, con el correo normalizado, y cada cambio de estado
 * queda fechado. Por eso no expone setters.
 * <p>
 * Integridad de datos en tres capas: validación del formulario (web), invariantes de esta entidad
 * (dominio) y restricciones de la base de datos (NOT NULL, UNIQUE, CHECK y versión optimista).
 */
@Entity
@Table(name = "practicantes")
@Check(name = "ck_practicantes_semestre", constraints = "semestre BETWEEN 1 AND 10")
public class Practicante {

    public static final int LONGITUD_MAXIMA_TEXTO = 120;
    public static final int SEMESTRE_MINIMO = 1;
    public static final int SEMESTRE_MAXIMO = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = LONGITUD_MAXIMA_TEXTO)
    private String nombreCompleto;

    @Column(nullable = false, unique = true, length = LONGITUD_MAXIMA_TEXTO)
    private String correo;

    @Column(nullable = false, length = LONGITUD_MAXIMA_TEXTO)
    private String carrera;

    @Column(nullable = false)
    private Integer semestre;

    /** Nombre con el que el practicante subió el archivo (solo para mostrar/descargar). */
    @Column(nullable = false)
    private String hojaVidaNombreOriginal;

    /** Identificador interno (UUID) con el que se guardó en el almacenamiento. */
    @Column(nullable = false, unique = true)
    private String hojaVidaArchivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPracticante estado;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    private LocalDateTime fechaActualizacionEstado;

    /**
     * Bloqueo optimista: JPA incrementa la versión en cada cambio y rechaza la escritura si otra
     * transacción la modificó antes (dos analistas decidiendo a la vez). Se publica como ETag.
     */
    @Version
    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0 NOT NULL")
    private long version;

    /** Requerido por JPA. */
    protected Practicante() {
    }

    private Practicante(String nombreCompleto, String correo, String carrera, int semestre,
                        String hojaVidaNombreOriginal, String hojaVidaArchivo) {
        this.nombreCompleto = nombreCompleto;
        this.correo = correo;
        this.carrera = carrera;
        this.semestre = semestre;
        this.hojaVidaNombreOriginal = hojaVidaNombreOriginal;
        this.hojaVidaArchivo = hojaVidaArchivo;
        this.estado = EstadoPracticante.PENDIENTE;
    }

    /** Método de fábrica: única forma de crear un practicante válido. */
    public static Practicante registrar(String nombreCompleto, String correo, String carrera, int semestre,
                                        String hojaVidaNombreOriginal, String hojaVidaArchivo) {
        return new Practicante(
                Objects.requireNonNull(nombreCompleto, "nombreCompleto").trim(),
                normalizarCorreo(correo),
                Objects.requireNonNull(carrera, "carrera").trim(),
                validarSemestre(semestre),
                Objects.requireNonNull(hojaVidaNombreOriginal, "hojaVidaNombreOriginal"),
                Objects.requireNonNull(hojaVidaArchivo, "hojaVidaArchivo"));
    }

    /** El correo se compara y guarda sin espacios y en minúsculas (Locale.ROOT evita sorpresas regionales). */
    public static String normalizarCorreo(String correo) {
        return Objects.requireNonNull(correo, "correo").trim().toLowerCase(Locale.ROOT);
    }

    private static int validarSemestre(int semestre) {
        if (semestre < SEMESTRE_MINIMO || semestre > SEMESTRE_MAXIMO) {
            throw new SemestreFueraDeRangoException();
        }
        return semestre;
    }

    /**
     * Decisión del analista: el estado y su fecha cambian siempre juntos, y solo hacia
     * un estado permitido por {@link EstadoPracticante#transicionesPermitidas()}.
     */
    public void cambiarEstado(EstadoPracticante nuevoEstado) {
        Objects.requireNonNull(nuevoEstado, "El estado es obligatorio");
        if (!estado.puedeCambiarA(nuevoEstado)) {
            throw new TransicionDeEstadoInvalidaException(estado, nuevoEstado);
        }
        this.estado = nuevoEstado;
        this.fechaActualizacionEstado = LocalDateTime.now();
    }

    /** ¿La versión que conoce el cliente sigue siendo la actual? */
    public boolean tieneVersion(long versionEsperada) {
        return version == versionEsperada;
    }

    @PrePersist
    void alCrear() {
        fechaRegistro = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public String getCarrera() {
        return carrera;
    }

    public Integer getSemestre() {
        return semestre;
    }

    public String getHojaVidaNombreOriginal() {
        return hojaVidaNombreOriginal;
    }

    public String getHojaVidaArchivo() {
        return hojaVidaArchivo;
    }

    public EstadoPracticante getEstado() {
        return estado;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public LocalDateTime getFechaActualizacionEstado() {
        return fechaActualizacionEstado;
    }

    public long getVersion() {
        return version;
    }
}
