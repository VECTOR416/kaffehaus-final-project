package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Usuario.Domain.ValueObjects.UsuarioId;

public class ExistenciaJpaMapper {
    private ExistenciaJpaMapper() {
    }

    public static ExistenciaJpaModel toPersistence(Existencia existencia) {
        ExistenciaJpaModel model = new ExistenciaJpaModel();
        model.setId(existencia.getId() != null ? UUID.fromString(existencia.getId().getValue()) : UUID.randomUUID());
        model.setContenidoInicial(existencia.getContenidoInicial());
        model.setContenidoActual(existencia.getContenidoActual());
        model.setFechaAcabado(existencia.getFechaAcabado());
        model.setFechaVencimiento(existencia.getFechaVencimiento());
        model.setFechaApertura(existencia.getFechaApertura());
        model.setIdDetalleLote(UUID.fromString(existencia.getDetalleLoteId().getValue()));
        model.setIdEstadoExistencia(UUID.fromString(existencia.getEstadoExistenciaId().getValue()));
        model.setUnidad(UUID.fromString(existencia.getUnidadId().getValue()));
        model.setIdUsuario(existencia.getUsuarioId() != null ? UUID.fromString(existencia.getUsuarioId().getValue()) : null);
        model.setBorrado(existencia.isBorrado());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(Existencia existencia, ExistenciaJpaModel model) {
        model.setContenidoInicial(existencia.getContenidoInicial());
        model.setContenidoActual(existencia.getContenidoActual());
        model.setFechaAcabado(existencia.getFechaAcabado());
        model.setFechaVencimiento(existencia.getFechaVencimiento());
        model.setFechaApertura(existencia.getFechaApertura());
        model.setIdDetalleLote(UUID.fromString(existencia.getDetalleLoteId().getValue()));
        model.setIdEstadoExistencia(UUID.fromString(existencia.getEstadoExistenciaId().getValue()));
        model.setUnidad(UUID.fromString(existencia.getUnidadId().getValue()));
        model.setIdUsuario(existencia.getUsuarioId() != null ? UUID.fromString(existencia.getUsuarioId().getValue()) : null);
        model.setBorrado(existencia.isBorrado());
        model.setFecU(OffsetDateTime.now());
    }

    public static Existencia toDomain(ExistenciaJpaModel model) {
        return Existencia.restaurar(
                model.getId().toString(),
                model.getContenidoInicial(),
                model.getContenidoActual(),
                model.getFechaAcabado(),
                model.getFechaVencimiento(),
                model.getFechaApertura(),
                new DetalleLoteId(model.getIdDetalleLote().toString()),
                new EstadoExistenciaId(model.getIdEstadoExistencia().toString()),
                new UnidadId(model.getUnidad().toString()),
                model.getIdUsuario() != null ? new UsuarioId(model.getIdUsuario().toString()) : null,
                model.getBorrado() != null && model.getBorrado());
    }
}
