package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;

public class EntidadJpaMapper {
    private EntidadJpaMapper() {
    }

    public static EntidadesJpaModel toPersistence(Entidad entidad) {
        EntidadesJpaModel model = new EntidadesJpaModel();
        model.setId(entidad.getId() != null ? UUID.fromString(entidad.getId().getValue()) : UUID.randomUUID());
        model.setNombre(entidad.getNombre());
        model.setNit(entidad.getNit());
        model.setTelefono(entidad.getTelefono());
        model.setDescripcion(entidad.getDescripcion());
        model.setBorrado(entidad.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Entidad entidad, EntidadesJpaModel model) {
        model.setNombre(entidad.getNombre());
        model.setNit(entidad.getNit());
        model.setTelefono(entidad.getTelefono());
        model.setDescripcion(entidad.getDescripcion());
        model.setBorrado(entidad.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Entidad toDomain(EntidadesJpaModel model) {
        return Entidad.restaurar(
                model.getId().toString(),
                model.getNombre(),
                model.getNit(),
                model.getTelefono(),
                model.getDescripcion(),
                model.getBorrado() != null && model.getBorrado());
    }
}
