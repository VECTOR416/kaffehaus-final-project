package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadJpaRepository extends JpaRepository<UnidadJpaModel, UUID>{

    Optional<UnidadJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<UnidadJpaModel> findByBorradoFalse(Pageable pageable);
    Page<UnidadJpaModel> findByBorradoFalseAndUnidadContainingIgnoreCase(String unidad, Pageable pageable);
}
