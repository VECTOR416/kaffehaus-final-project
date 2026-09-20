package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;

public class MovimientoJpaMapper {
    private MovimientoJpaMapper() {
    }

    public static MovimientoJpaModel toPersistence(Movimiento movimiento) {
        MovimientoJpaModel model = new MovimientoJpaModel();
        model.setId(movimiento.getId() != null ? UUID.fromString(movimiento.getId().getValue()) : UUID.randomUUID());
        model.setCantidad(movimiento.getCantidad());
        model.setFechaMovimiento(movimiento.getFechaMovimiento());
        model.setMotivo(movimiento.getMotivo());
        model.setIdExistencia(UUID.fromString(movimiento.getExistenciaId().getValue()));
        model.setIdTipoMovimiento(UUID.fromString(movimiento.getTipoMovimientoId().getValue()));
        model.setIdUsuario(UUID.fromString(movimiento.getUsuarioId().getValue()));
        model.setBorrado(movimiento.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Movimiento movimiento, MovimientoJpaModel model) {
        model.setCantidad(movimiento.getCantidad());
        model.setFechaMovimiento(movimiento.getFechaMovimiento());
        model.setMotivo(movimiento.getMotivo());
        model.setIdExistencia(UUID.fromString(movimiento.getExistenciaId().getValue()));
        model.setIdTipoMovimiento(UUID.fromString(movimiento.getTipoMovimientoId().getValue()));
        model.setIdUsuario(UUID.fromString(movimiento.getUsuarioId().getValue()));
        model.setBorrado(movimiento.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Movimiento toDomain(MovimientoJpaModel model) {
        return Movimiento.restaurar(
                model.getId().toString(),
                model.getCantidad(),
                model.getFechaMovimiento(),
                model.getMotivo(),
                new ExistenciaId(model.getIdExistencia().toString()),
                new TipoMovimientoId(model.getIdTipoMovimiento().toString()),
                new UsuarioId(model.getIdUsuario().toString()),
                model.getBorrado() != null && model.getBorrado());
    }
}
