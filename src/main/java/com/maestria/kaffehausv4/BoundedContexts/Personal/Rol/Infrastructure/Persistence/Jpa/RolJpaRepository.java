package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolJpaRepository extends JpaRepository<RolJpaModel, UUID>{

    Optional<RolJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<RolJpaModel> findByBorradoFalse(Pageable pageable);
    Page<RolJpaModel> findByBorradoFalseAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
