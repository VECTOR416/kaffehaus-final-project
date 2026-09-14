package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;

public class EstadoVentaJpaMapper {
    private EstadoVentaJpaMapper() {
    }

    public static EstadoVentaJpaModel toPersistence(EstadoVenta estadoVenta) {
        EstadoVentaJpaModel model = new EstadoVentaJpaModel();
        model.setId(estadoVenta.getId() != null ? UUID.fromString(estadoVenta.getId().getValue()) : UUID.randomUUID());
        model.setNombre(estadoVenta.getNombre());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(EstadoVenta estadoVenta, EstadoVentaJpaModel model) {
        model.setNombre(estadoVenta.getNombre());
        model.setFecU(OffsetDateTime.now());
    }

    public static EstadoVenta toDomain(EstadoVentaJpaModel model) {
        return EstadoVenta.restaurar(
                model.getId().toString(),
                model.getNombre());
    }
}
