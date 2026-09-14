package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoMovimientoJpaRepository extends JpaRepository<TipoMovimientoJpaModel, UUID>{

    Optional<TipoMovimientoJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<TipoMovimientoJpaModel> findByBorradoFalse(Pageable pageable);
    Page<TipoMovimientoJpaModel> findByBorradoFalseAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
