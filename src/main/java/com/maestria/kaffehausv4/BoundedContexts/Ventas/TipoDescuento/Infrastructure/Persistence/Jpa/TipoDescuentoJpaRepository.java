package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoDescuentoJpaRepository extends JpaRepository<TipoDescuentoJpaModel, UUID>{

    Optional<TipoDescuentoJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<TipoDescuentoJpaModel> findByBorradoFalse(Pageable pageable);
    Page<TipoDescuentoJpaModel> findByBorradoFalseAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
    Optional<TipoDescuentoJpaModel> findFirstByNombreIgnoreCaseAndBorradoFalse(String nombre);
}
