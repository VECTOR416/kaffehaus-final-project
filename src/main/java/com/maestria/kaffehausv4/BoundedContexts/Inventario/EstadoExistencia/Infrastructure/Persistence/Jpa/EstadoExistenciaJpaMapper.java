package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Infrastructure.Persistence.Jpa;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;

public class EstadoExistenciaJpaMapper {
    private EstadoExistenciaJpaMapper() {
    }

    public static EstadoExistenciaJpaModel toPersistence(EstadoExistencia estadoExistencia) {
        EstadoExistenciaJpaModel model = new EstadoExistenciaJpaModel();
        model.setId(estadoExistencia.getId() != null ? UUID.fromString(estadoExistencia.getId().getValue()) : UUID.randomUUID());
        model.setNombre(estadoExistencia.getNombre());
        model.setFecC(OffsetDateTime.now());

        return model;
    }

    public static void copiarCambios(EstadoExistencia estadoExistencia, EstadoExistenciaJpaModel model) {
        model.setNombre(estadoExistencia.getNombre());
        model.setFecU(OffsetDateTime.now());
    }

    public static EstadoExistencia toDomain(EstadoExistenciaJpaModel model) {
        return EstadoExistencia.restaurar(
                model.getId().toString(),
                model.getNombre());
    }
}
