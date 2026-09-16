package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories.IExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerExistenciaPorIdUseCase {
    private final IExistenciaRepository existenciaRepository;
    private final IEstadoExistenciaRepository estadoExistenciaRepository;
    private final IUnidadRepository unidadRepository;

    public ObtenerExistenciaPorIdUseCase(
            IExistenciaRepository existenciaRepository,
            IEstadoExistenciaRepository estadoExistenciaRepository,
            IUnidadRepository unidadRepository) {
        this.existenciaRepository = existenciaRepository;
        this.estadoExistenciaRepository = estadoExistenciaRepository;
        this.unidadRepository = unidadRepository;
    }

    public ExistenciaResponse execute(String existenciaId) {
        ExistenciaId id = new ExistenciaId(existenciaId);

        Existencia existencia = existenciaRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La existencia con id '" + existenciaId + "' no existe"));

        return toResponse(existencia);
    }

    private ExistenciaResponse toResponse(Existencia existencia) {
        EstadoExistencia estadoExistencia = estadoExistenciaRepository.obtenerPorId(existencia.getEstadoExistenciaId())
                .orElseThrow(() -> DomainException.throwException(404, "El estado de existencia asociado a la existencia no existe"));
        Unidad unidad = unidadRepository.obtenerPorId(existencia.getUnidadId())
                .orElseThrow(() -> DomainException.throwException(404, "La unidad asociada a la existencia no existe"));
        return ExistenciaResponse.createFromAggregate(existencia, estadoExistencia, unidad);
    }
}
