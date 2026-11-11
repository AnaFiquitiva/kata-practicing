package com.bdb.katapracticing.practicantes.persistencia;

import com.bdb.katapracticing.practicantes.dominio.EstadoPracticante;
import com.bdb.katapracticing.practicantes.dominio.Practicante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticanteRepository extends JpaRepository<Practicante, Long> {

    boolean existsByCorreoIgnoreCase(String correo);

    List<Practicante> findAllByOrderByFechaRegistroDesc();

    List<Practicante> findByEstadoOrderByFechaRegistroDesc(EstadoPracticante estado);
}
