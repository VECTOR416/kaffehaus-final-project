package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaJpaRepository extends JpaRepository<VentaJpaModel, UUID>{

    Optional<VentaJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<VentaJpaModel> findByBorradoFalse(Pageable pageable);
}
