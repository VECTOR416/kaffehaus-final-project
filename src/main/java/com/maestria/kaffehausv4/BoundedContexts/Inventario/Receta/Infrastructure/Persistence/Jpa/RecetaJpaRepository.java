package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecetaJpaRepository extends JpaRepository<RecetaJpaModel, UUID>{

    Optional<RecetaJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<RecetaJpaModel> findByBorradoFalse(Pageable pageable);
    Page<RecetaJpaModel> findByBorradoFalseAndTituloContainingIgnoreCase(String titulo, Pageable pageable);
    Optional<RecetaJpaModel> findFirstByIdProductoObtenidoAndBorradoFalseOrderByFecCDesc(UUID idProductoObtenido);
}
