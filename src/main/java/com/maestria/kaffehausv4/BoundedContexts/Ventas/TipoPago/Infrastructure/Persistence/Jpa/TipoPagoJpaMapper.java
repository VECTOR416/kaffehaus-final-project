package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoPago.Domain.Aggregates.TipoPago;

public class TipoPagoJpaMapper {
    private TipoPagoJpaMapper() {
    }

    public static TipoPagoJpaModel toPersistence(TipoPago tipoPago) {
        TipoPagoJpaModel model = new TipoPagoJpaModel();
        model.setId(tipoPago.getId() != null ? UUID.fromString(tipoPago.getId().getValue()) : UUID.randomUUID());
        model.setNombre(tipoPago.getNombre());
        model.setBorrado(tipoPago.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(TipoPago tipoPago, TipoPagoJpaModel model) {
        model.setNombre(tipoPago.getNombre());
        model.setBorrado(tipoPago.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static TipoPago toDomain(TipoPagoJpaModel model) {
        return TipoPago.restaurar(
                model.getId().toString(),
                model.getNombre(),
                model.getBorrado() != null && model.getBorrado());
    }
}
