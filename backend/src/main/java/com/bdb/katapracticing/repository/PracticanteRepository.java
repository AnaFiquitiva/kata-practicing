package com.bdb.katapracticing.repository;

import com.bdb.katapracticing.model.EstadoPracticante;
import com.bdb.katapracticing.model.Practicante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticanteRepository extends JpaRepository<Practicante, Long> {

    boolean existsByCorreoIgnoreCase(String correo);

    List<Practicante> findAllByOrderByFechaRegistroDesc();

    List<Practicante> findByEstadoOrderByFechaRegistroDesc(EstadoPracticante estado);
}
