package com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Rol.Domain.Aggregates.Rol;

public class RolJpaMapper {
    private RolJpaMapper() {
    }

    public static RolJpaModel toPersistence(Rol rol) {
        RolJpaModel model = new RolJpaModel();
        model.setId(rol.getId() != null ? UUID.fromString(rol.getId().getValue()) : UUID.randomUUID());
        model.setNombre(rol.getNombre());
        model.setBorrado(rol.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Rol rol, RolJpaModel model) {
        model.setNombre(rol.getNombre());
        model.setBorrado(rol.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Rol toDomain(RolJpaModel model) {
        return Rol.restaurar(
                model.getId().toString(),
                model.getNombre(),
                model.getBorrado() != null && model.getBorrado());
    }
}
