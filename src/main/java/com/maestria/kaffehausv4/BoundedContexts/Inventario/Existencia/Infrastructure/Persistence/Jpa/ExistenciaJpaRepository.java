package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExistenciaJpaRepository extends JpaRepository<ExistenciaJpaModel, UUID>{

    Optional<ExistenciaJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<ExistenciaJpaModel> findByBorradoFalse(Pageable pageable);
}
