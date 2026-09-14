package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.Aggregates.Produccion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;

public class ProduccionJpaMapper {
    private ProduccionJpaMapper() {
    }

    public static ProduccionJpaModel toPersistence(Produccion produccion) {
        ProduccionJpaModel model = new ProduccionJpaModel();
        model.setId(produccion.getId() != null ? UUID.fromString(produccion.getId().getValue()) : UUID.randomUUID());
        model.setCantidadEsperada(produccion.getCantidadEsperada());
        model.setCantidadReal(produccion.getCantidadReal());
        model.setDiferencia(produccion.getDiferencia());
        model.setMotivoDiferencia(produccion.getMotivoDiferencia());
        model.setFechaProduccion(produccion.getFechaProduccion());
        model.setIdReceta(UUID.fromString(produccion.getRecetaId().getValue()));
        model.setBorrado(produccion.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Produccion produccion, ProduccionJpaModel model) {
        model.setCantidadEsperada(produccion.getCantidadEsperada());
        model.setCantidadReal(produccion.getCantidadReal());
        model.setDiferencia(produccion.getDiferencia());
        model.setMotivoDiferencia(produccion.getMotivoDiferencia());
        model.setFechaProduccion(produccion.getFechaProduccion());
        model.setIdReceta(UUID.fromString(produccion.getRecetaId().getValue()));
        model.setBorrado(produccion.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Produccion toDomain(ProduccionJpaModel model) {
        return Produccion.restaurar(
                model.getId().toString(),
                model.getCantidadEsperada(),
                model.getCantidadReal(),
                model.getDiferencia(),
                model.getMotivoDiferencia(),
                model.getFechaProduccion(),
                new RecetaId(model.getIdReceta().toString()),
                model.getBorrado() != null && model.getBorrado());
    }
}
