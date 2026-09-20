package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;

public class TipoJpaMapper {
    private TipoJpaMapper() {
    }

    public static TipoJpaModel toPersistence(Tipo tipo) {
        TipoJpaModel model = new TipoJpaModel();
        model.setId(tipo.getId() != null ? UUID.fromString(tipo.getId().getValue()) : UUID.randomUUID());
        model.setNombre(tipo.getNombre());
        model.setBorrado(tipo.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Tipo tipo, TipoJpaModel model) {
        model.setNombre(tipo.getNombre());
        model.setBorrado(tipo.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Tipo toDomain(TipoJpaModel model) {
        return Tipo.restaurar(
                model.getId().toString(),
                model.getNombre(),
                model.getBorrado() != null && model.getBorrado());
    }
}
