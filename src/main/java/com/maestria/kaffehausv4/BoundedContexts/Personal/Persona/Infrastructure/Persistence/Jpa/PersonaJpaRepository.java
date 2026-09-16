package com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Infrastructure.Persistence.Jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaJpaRepository extends JpaRepository<PersonaJpaModel, UUID> {

    Optional<PersonaJpaModel> findByIdAndBorradoFalse(UUID id);
}
