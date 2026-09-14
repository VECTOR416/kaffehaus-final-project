package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasoRecetaJpaRepository extends JpaRepository<PasoRecetaJpaModel, UUID>{

    Optional<PasoRecetaJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<PasoRecetaJpaModel> findByBorradoFalse(Pageable pageable);
    Page<PasoRecetaJpaModel> findByBorradoFalseAndDescripcionContainingIgnoreCase(String descripcion, Pageable pageable);
}
