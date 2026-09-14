package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;

public class UnidadJpaMapper {
    private UnidadJpaMapper() {
    }

    public static UnidadJpaModel toPersistence(Unidad unidad) {
        UnidadJpaModel model = new UnidadJpaModel();
        model.setId(unidad.getId() != null ? UUID.fromString(unidad.getId().getValue()) : UUID.randomUUID());
        model.setUnidad(unidad.getUnidad());
        model.setAbreviatura(unidad.getAbreviatura());
        model.setEsBorrable(unidad.isEsBorrable());
        model.setBorrado(unidad.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Unidad unidad, UnidadJpaModel model) {
        model.setUnidad(unidad.getUnidad());
        model.setAbreviatura(unidad.getAbreviatura());
        model.setEsBorrable(unidad.isEsBorrable());
        model.setBorrado(unidad.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Unidad toDomain(UnidadJpaModel model) {
        return Unidad.restaurar(
                model.getId().toString(),
                model.getUnidad(),
                model.getAbreviatura(),
                model.getEsBorrable(),
                model.getBorrado() != null && model.getBorrado());
    }
}
