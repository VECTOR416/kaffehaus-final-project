package com.maestria.kaffehausv4.BoundedContexts.Ventas.Descuento.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DescuentoJpaRepository extends JpaRepository<DescuentoJpaModel, UUID>{

    Optional<DescuentoJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<DescuentoJpaModel> findByBorradoFalse(Pageable pageable);
}
