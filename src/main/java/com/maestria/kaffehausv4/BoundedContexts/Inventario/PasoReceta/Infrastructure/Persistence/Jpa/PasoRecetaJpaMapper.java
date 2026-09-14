package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.Aggregates.PasoReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;

public class PasoRecetaJpaMapper {
    private PasoRecetaJpaMapper() {
    }

    public static PasoRecetaJpaModel toPersistence(PasoReceta pasoReceta) {
        PasoRecetaJpaModel model = new PasoRecetaJpaModel();
        model.setId(pasoReceta.getId() != null ? UUID.fromString(pasoReceta.getId().getValue()) : UUID.randomUUID());
        model.setPasos(pasoReceta.getPasos());
        model.setDescripcion(pasoReceta.getDescripcion());
        model.setIdReceta(UUID.fromString(pasoReceta.getRecetaId().getValue()));
        model.setBorrado(pasoReceta.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(PasoReceta pasoReceta, PasoRecetaJpaModel model) {
        model.setPasos(pasoReceta.getPasos());
        model.setDescripcion(pasoReceta.getDescripcion());
        model.setIdReceta(UUID.fromString(pasoReceta.getRecetaId().getValue()));
        model.setBorrado(pasoReceta.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static PasoReceta toDomain(PasoRecetaJpaModel model) {
        return PasoReceta.restaurar(
                model.getId().toString(),
                model.getPasos(),
                model.getDescripcion(),
                new RecetaId(model.getIdReceta().toString()),
                model.getBorrado() != null && model.getBorrado());
    }
}
