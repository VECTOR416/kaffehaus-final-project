package com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos.EstadoExistenciaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Dtos.ObtenerEstadosExistenciaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Application.Repositories.IEstadoExistenciaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.EstadoExistencia.Domain.Aggregates.EstadoExistencia;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerEstadosExistenciaUseCase {
    private final IEstadoExistenciaRepository estadoExistenciaRepository;

    public ObtenerEstadosExistenciaUseCase(
            IEstadoExistenciaRepository estadoExistenciaRepository) {
        this.estadoExistenciaRepository = estadoExistenciaRepository;
    }

    public PaginatedResponse<List<EstadoExistenciaResponse>> execute(ObtenerEstadosExistenciaRequest obtenerEstadosExistenciaRequest) {
        PaginatedResponse<List<EstadoExistencia>> resultado = estadoExistenciaRepository.obtenerTodos(obtenerEstadosExistenciaRequest);

        List<EstadoExistenciaResponse> estadosExistencia = resultado.getData().stream()
                .map(EstadoExistenciaResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                estadosExistencia,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
