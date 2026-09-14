package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProduccionJpaRepository extends JpaRepository<ProduccionJpaModel, UUID>{

    Optional<ProduccionJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<ProduccionJpaModel> findByBorradoFalse(Pageable pageable);
    Page<ProduccionJpaModel> findByBorradoFalseAndMotivoDiferenciaContainingIgnoreCase(String motivoDiferencia, Pageable pageable);
}
