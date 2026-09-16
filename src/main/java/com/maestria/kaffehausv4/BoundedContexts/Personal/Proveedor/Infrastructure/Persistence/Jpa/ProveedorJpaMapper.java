package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects.EntidadId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.Aggregates.Proveedor;

public class ProveedorJpaMapper {
    private ProveedorJpaMapper() {
    }

    public static ProveedorJpaModel toPersistence(Proveedor proveedor) {
        ProveedorJpaModel model = new ProveedorJpaModel();
        model.setId(proveedor.getId() != null ? UUID.fromString(proveedor.getId().getValue()) : UUID.randomUUID());
        model.setIdPersona(UUID.fromString(proveedor.getPersonaId().getValue()));
        model.setIdEntidad(proveedor.getEntidadId() != null ? UUID.fromString(proveedor.getEntidadId().getValue()) : null);
        model.setBorrado(proveedor.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Proveedor proveedor, ProveedorJpaModel model) {
        model.setIdPersona(UUID.fromString(proveedor.getPersonaId().getValue()));
        model.setIdEntidad(proveedor.getEntidadId() != null ? UUID.fromString(proveedor.getEntidadId().getValue()) : null);
        model.setBorrado(proveedor.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Proveedor toDomain(ProveedorJpaModel model) {
        return Proveedor.restaurar(
                model.getId().toString(),
                new PersonaId(model.getIdPersona().toString()),
                model.getIdEntidad() != null ? new EntidadId(model.getIdEntidad().toString()) : null,
                null,
                null,
                model.getBorrado() != null && model.getBorrado());
    }
}
