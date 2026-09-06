package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Infrastructure.Persistence.Jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteJpaRepository extends  JpaRepository<ClienteJpaModel, UUID>{

}
