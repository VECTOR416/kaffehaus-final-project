package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;

public class TipoMovimientoJpaMapper {
    private TipoMovimientoJpaMapper() {
    }

    public static TipoMovimientoJpaModel toPersistence(TipoMovimiento tipoMovimiento) {
        TipoMovimientoJpaModel model = new TipoMovimientoJpaModel();
        model.setId(tipoMovimiento.getId() != null ? UUID.fromString(tipoMovimiento.getId().getValue()) : UUID.randomUUID());
        model.setNombre(tipoMovimiento.getNombre());
        model.setBorrado(tipoMovimiento.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(TipoMovimiento tipoMovimiento, TipoMovimientoJpaModel model) {
        model.setNombre(tipoMovimiento.getNombre());
        model.setBorrado(tipoMovimiento.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static TipoMovimiento toDomain(TipoMovimientoJpaModel model) {
        return TipoMovimiento.restaurar(
                model.getId().toString(),
                model.getNombre(),
                model.getBorrado() != null && model.getBorrado());
    }
}
