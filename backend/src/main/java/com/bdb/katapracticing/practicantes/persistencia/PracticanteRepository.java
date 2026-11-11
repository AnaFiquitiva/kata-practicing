package com.bdb.katapracticing.practicantes.persistencia;

import com.bdb.katapracticing.practicantes.dominio.EstadoPracticante;
import com.bdb.katapracticing.practicantes.dominio.Practicante;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticanteRepository extends JpaRepository<Practicante, Long> {

    boolean existsByCorreoIgnoreCase(String correo);

    Page<Practicante> findByEstado(EstadoPracticante estado, Pageable pagina);
}
