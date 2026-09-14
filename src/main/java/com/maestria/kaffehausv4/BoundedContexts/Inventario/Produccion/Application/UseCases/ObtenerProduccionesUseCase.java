package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ObtenerProduccionesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ProduccionResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Repositories.IProduccionRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.Aggregates.Produccion;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerProduccionesUseCase {
    private final IProduccionRepository produccionRepository;

    public ObtenerProduccionesUseCase(
            IProduccionRepository produccionRepository) {
        this.produccionRepository = produccionRepository;
    }

    public PaginatedResponse<List<ProduccionResponse>> execute(ObtenerProduccionesRequest obtenerProduccionesRequest) {
        PaginatedResponse<List<Produccion>> resultado = produccionRepository.obtenerTodos(obtenerProduccionesRequest);

        List<ProduccionResponse> producciones = resultado.getData().stream()
                .map(ProduccionResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                producciones,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
