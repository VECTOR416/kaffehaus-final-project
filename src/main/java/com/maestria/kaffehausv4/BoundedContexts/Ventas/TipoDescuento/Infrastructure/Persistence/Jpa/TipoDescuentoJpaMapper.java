package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;

public class TipoDescuentoJpaMapper {
    private TipoDescuentoJpaMapper() {
    }

    public static TipoDescuentoJpaModel toPersistence(TipoDescuento tipoDescuento) {
        TipoDescuentoJpaModel model = new TipoDescuentoJpaModel();
        model.setId(tipoDescuento.getId() != null ? UUID.fromString(tipoDescuento.getId().getValue()) : UUID.randomUUID());
        model.setNombre(tipoDescuento.getNombre());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(TipoDescuento tipoDescuento, TipoDescuentoJpaModel model) {
        model.setNombre(tipoDescuento.getNombre());
        model.setFecU(OffsetDateTime.now());
    }

    public static TipoDescuento toDomain(TipoDescuentoJpaModel model) {
        return TipoDescuento.restaurar(
                model.getId().toString(),
                model.getNombre());
    }
}
