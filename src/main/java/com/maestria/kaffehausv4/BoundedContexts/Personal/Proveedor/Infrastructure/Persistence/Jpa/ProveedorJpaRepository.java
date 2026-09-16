package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorJpaRepository extends JpaRepository<ProveedorJpaModel, UUID>{

    Optional<ProveedorJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<ProveedorJpaModel> findByBorradoFalse(Pageable pageable);
}
