package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteJpaRepository extends  JpaRepository<ClienteJpaModel, UUID>{

    Optional<ClienteJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<ClienteJpaModel> findByBorradoFalse(Pageable pageable);
}
