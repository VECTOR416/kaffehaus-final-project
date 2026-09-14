package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoteJpaRepository extends JpaRepository<LoteJpaModel, UUID>{

    Optional<LoteJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<LoteJpaModel> findByBorradoFalse(Pageable pageable);
}
