package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos.CrearEstadoExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos.EstadoExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;

@Service
@Transactional
public class CrearEstadoExistenciaUseCase {
    private final IEstadoExistenciaRepository estadoExistenciaRepository;

    public CrearEstadoExistenciaUseCase(
            IEstadoExistenciaRepository estadoExistenciaRepository) {
        this.estadoExistenciaRepository = estadoExistenciaRepository;
    }

    public EstadoExistenciaResponse execute(CrearEstadoExistenciaRequest crearEstadoExistenciaRequest) {
        EstadoExistencia estadoExistencia = EstadoExistencia.crear(
                crearEstadoExistenciaRequest.getNombre());

        EstadoExistencia estadoExistenciaCreado = estadoExistenciaRepository.crear(estadoExistencia);

        return EstadoExistenciaResponse.createFromAggregate(estadoExistenciaCreado);
    }
}
