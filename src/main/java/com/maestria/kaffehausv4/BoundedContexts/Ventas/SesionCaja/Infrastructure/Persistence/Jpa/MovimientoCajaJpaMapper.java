package com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.SesionCaja.Domain.Aggregates.MovimientoCaja;

/** La FK id_sesion_caja la pone el datasource a partir de la raiz: la hija no la conoce. */
public class MovimientoCajaJpaMapper {
    private MovimientoCajaJpaMapper() {
    }

    public static MovimientoCajaJpaModel toPersistence(MovimientoCaja movimiento, UUID idSesionCaja) {
        MovimientoCajaJpaModel model = new MovimientoCajaJpaModel();
        model.setId(movimiento.getId() != null ? UUID.fromString(movimiento.getId().getValue()) : UUID.randomUUID());
        model.setIdSesionCaja(idSesionCaja);
        model.setIdUsuario(UUID.fromString(movimiento.getUsuarioId().getValue()));
        model.setTipo(movimiento.getTipo());
        model.setMonto(movimiento.getMonto());
        model.setMotivo(movimiento.getMotivo());
        model.setBorrado(movimiento.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(MovimientoCaja movimiento, MovimientoCajaJpaModel model) {
        model.setBorrado(movimiento.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static MovimientoCaja toDomain(MovimientoCajaJpaModel model) {
        return MovimientoCaja.restaurar(
                model.getId().toString(),
                new UsuarioId(model.getIdUsuario().toString()),
                model.getTipo(),
                model.getMonto(),
                model.getMotivo(),
                model.getFecC() != null ? model.getFecC().toLocalDateTime() : null,
                model.getBorrado() != null && model.getBorrado());
    }
}
