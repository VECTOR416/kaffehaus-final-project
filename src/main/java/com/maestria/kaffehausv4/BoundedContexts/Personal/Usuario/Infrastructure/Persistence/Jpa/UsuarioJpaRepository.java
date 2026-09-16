package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaModel, UUID>{

    Optional<UsuarioJpaModel> findByIdAndBorradoFalse(UUID id);
    Page<UsuarioJpaModel> findByBorradoFalse(Pageable pageable);
    Page<UsuarioJpaModel> findByBorradoFalseAndCorreoContainingIgnoreCase(String correo, Pageable pageable);
}
