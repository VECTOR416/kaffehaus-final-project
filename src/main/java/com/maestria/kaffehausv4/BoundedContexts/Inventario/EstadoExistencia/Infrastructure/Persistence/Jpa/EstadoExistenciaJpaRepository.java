package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoExistenciaJpaRepository extends JpaRepository<EstadoExistenciaJpaModel, UUID>{

    Optional<EstadoExistenciaJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<EstadoExistenciaJpaModel> findByBorradoFalse(Pageable pageable);
    Page<EstadoExistenciaJpaModel> findByBorradoFalseAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
    Optional<EstadoExistenciaJpaModel> findFirstByNombreIgnoreCaseAndBorradoFalse(String nombre);
}
