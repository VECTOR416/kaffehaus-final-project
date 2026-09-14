package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredienteRecetaJpaRepository extends JpaRepository<IngredienteRecetaJpaModel, UUID>{

    Optional<IngredienteRecetaJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<IngredienteRecetaJpaModel> findByBorradoFalse(Pageable pageable);
}
