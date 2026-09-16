package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadJpaRepository extends JpaRepository<EntidadesJpaModel, UUID>{

    Optional<EntidadesJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<EntidadesJpaModel> findByBorradoFalse(Pageable pageable);
    Page<EntidadesJpaModel> findByBorradoFalseAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
