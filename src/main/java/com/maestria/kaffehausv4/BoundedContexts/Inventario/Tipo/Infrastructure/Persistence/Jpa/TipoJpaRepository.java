package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoJpaRepository extends JpaRepository<TipoJpaModel, UUID>{

    Optional<TipoJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<TipoJpaModel> findByBorradoFalse(Pageable pageable);
    Page<TipoJpaModel> findByBorradoFalseAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
    Optional<TipoJpaModel> findFirstByNombreIgnoreCaseAndBorradoFalse(String nombre);
}
