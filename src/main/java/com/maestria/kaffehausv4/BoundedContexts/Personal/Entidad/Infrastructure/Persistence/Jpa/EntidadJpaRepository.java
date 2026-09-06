package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Infrastructure.Persistence.Jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntidadJpaRepository extends JpaRepository<EntidadesJpaModel, UUID>{

}
