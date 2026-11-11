package com.bdb.katapracticing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "practicantes")
public class Practicante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombreCompleto;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @Column(nullable = false, length = 120)
    private String carrera;

    @Column(nullable = false)
    private Integer semestre;

    /** Nombre con el que el practicante subió el archivo (solo para mostrar/descargar). */
    @Column(nullable = false)
    private String hojaVidaNombreOriginal;

    /** Nombre interno (UUID) con el que se guardó en la carpeta del servidor. */
    @Column(nullable = false, unique = true)
    private String hojaVidaArchivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPracticante estado;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;

    private LocalDateTime fechaActualizacionEstado;

    @PrePersist
    void alCrear() {
        if (estado == null) {
            estado = EstadoPracticante.PENDIENTE;
        }
        fechaRegistro = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getCarrera() { return carrera; }
    public void setCarrera(String carrera) { this.carrera = carrera; }

    public Integer getSemestre() { return semestre; }
    public void setSemestre(Integer semestre) { this.semestre = semestre; }

    public String getHojaVidaNombreOriginal() { return hojaVidaNombreOriginal; }
    public void setHojaVidaNombreOriginal(String n) { this.hojaVidaNombreOriginal = n; }

    public String getHojaVidaArchivo() { return hojaVidaArchivo; }
    public void setHojaVidaArchivo(String hojaVidaArchivo) { this.hojaVidaArchivo = hojaVidaArchivo; }

    public EstadoPracticante getEstado() { return estado; }
    public void setEstado(EstadoPracticante estado) { this.estado = estado; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }

    public LocalDateTime getFechaActualizacionEstado() { return fechaActualizacionEstado; }
    public void setFechaActualizacionEstado(LocalDateTime f) { this.fechaActualizacionEstado = f; }
}
