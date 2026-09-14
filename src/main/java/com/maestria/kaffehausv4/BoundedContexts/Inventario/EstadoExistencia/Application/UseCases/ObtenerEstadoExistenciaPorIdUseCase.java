package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos.EstadoExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.ValueObjects.EstadoExistenciaId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerEstadoExistenciaPorIdUseCase {
    private final IEstadoExistenciaRepository estadoExistenciaRepository;

    public ObtenerEstadoExistenciaPorIdUseCase(
            IEstadoExistenciaRepository estadoExistenciaRepository) {
        this.estadoExistenciaRepository = estadoExistenciaRepository;
    }

    public EstadoExistenciaResponse execute(String estadoExistenciaId) {
        EstadoExistenciaId id = new EstadoExistenciaId(estadoExistenciaId);

        EstadoExistencia estadoExistencia = estadoExistenciaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia con id '" + estadoExistenciaId + "' no existe"));

        return EstadoExistenciaResponse.createFromAggregate(estadoExistencia);
    }
}
