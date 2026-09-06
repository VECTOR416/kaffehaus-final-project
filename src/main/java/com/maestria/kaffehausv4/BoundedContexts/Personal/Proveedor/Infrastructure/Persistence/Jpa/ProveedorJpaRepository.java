package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Infrastructure.Persistence.Jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorJpaRepository extends JpaRepository<ProveedorJpaModel, UUID>{

}
