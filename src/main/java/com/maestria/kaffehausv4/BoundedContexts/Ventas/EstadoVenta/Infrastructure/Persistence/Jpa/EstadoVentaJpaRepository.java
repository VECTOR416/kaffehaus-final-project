package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Infrastructure.Persistence.Jpa;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstadoVentaJpaRepository extends JpaRepository<EstadoVentaJpaModel, UUID>{

    Page<EstadoVentaJpaModel> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
