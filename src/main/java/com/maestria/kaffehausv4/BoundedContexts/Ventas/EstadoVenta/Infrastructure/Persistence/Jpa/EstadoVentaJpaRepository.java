package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoVentaJpaRepository extends JpaRepository<EstadoVentaJpaModel, UUID>{

    Optional<EstadoVentaJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<EstadoVentaJpaModel> findByBorradoFalse(Pageable pageable);
    Page<EstadoVentaJpaModel> findByBorradoFalseAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
    Optional<EstadoVentaJpaModel> findFirstByNombreIgnoreCaseAndBorradoFalse(String nombre);
}
