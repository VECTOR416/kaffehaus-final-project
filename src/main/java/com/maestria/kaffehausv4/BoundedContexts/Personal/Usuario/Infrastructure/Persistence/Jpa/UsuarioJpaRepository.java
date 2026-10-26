package com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaModel, UUID>{

    Optional<UsuarioJpaModel> findByIdAndBorradoFalse(UUID id);
    Optional<UsuarioJpaModel> findByCorreoIgnoreCaseAndBorradoFalse(String correo);
    Page<UsuarioJpaModel> findByBorradoFalse(Pageable pageable);
    Page<UsuarioJpaModel> findByBorradoFalseAndCorreoContainingIgnoreCase(String correo, Pageable pageable);

    // Usuarios vivos de un rol (logica.md GENERAL-18, USUARIO-04).
    long countByIdRolAndBorradoFalse(UUID idRol);
}
