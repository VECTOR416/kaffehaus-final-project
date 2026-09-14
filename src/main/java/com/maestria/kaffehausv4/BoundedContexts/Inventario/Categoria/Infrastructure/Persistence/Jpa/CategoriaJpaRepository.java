package com.maestria.kaffehausv4.BoundedContexts.Inventario.Categoria.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaJpaRepository extends JpaRepository<CategoriaJpaModel, UUID>{

    Optional<CategoriaJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<CategoriaJpaModel> findByBorradoFalse(Pageable pageable);
    Page<CategoriaJpaModel> findByBorradoFalseAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
