package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.EstadoVentaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.ObtenerEstadosVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories.IEstadoVentaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerEstadosVentaUseCase {
    private final IEstadoVentaRepository estadoVentaRepository;

    public ObtenerEstadosVentaUseCase(
            IEstadoVentaRepository estadoVentaRepository) {
        this.estadoVentaRepository = estadoVentaRepository;
    }

    public PaginatedResponse<List<EstadoVentaResponse>> execute(ObtenerEstadosVentaRequest obtenerEstadosVentaRequest) {
        PaginatedResponse<List<EstadoVenta>> resultado = estadoVentaRepository.obtenerTodos(obtenerEstadosVentaRequest);

        List<EstadoVentaResponse> estadosVenta = resultado.getData().stream()
                .map(EstadoVentaResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                estadosVenta,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
