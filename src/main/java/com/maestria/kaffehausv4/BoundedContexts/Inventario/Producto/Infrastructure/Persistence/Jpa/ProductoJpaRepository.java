package com.maestria.kaffehausv4.BoundedContexts.Inventario.Producto.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoJpaRepository extends JpaRepository<ProductoJpaModel, UUID>{

    Optional<ProductoJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<ProductoJpaModel> findByBorradoFalse(Pageable pageable);
    Page<ProductoJpaModel> findByBorradoFalseAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
