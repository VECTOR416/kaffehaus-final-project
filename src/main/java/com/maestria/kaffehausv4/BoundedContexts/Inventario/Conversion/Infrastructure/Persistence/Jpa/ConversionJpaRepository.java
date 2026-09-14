package com.maestria.kaffehausv4.BoundedContexts.Inventario.Conversion.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversionJpaRepository extends JpaRepository<ConversionJpaModel, UUID>{

    Optional<ConversionJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<ConversionJpaModel> findByBorradoFalse(Pageable pageable);
}
