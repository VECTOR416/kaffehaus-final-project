package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.ObtenerUnidadesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.UnidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories.IUnidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerUnidadesUseCase {
    private final IUnidadRepository unidadRepository;

    public ObtenerUnidadesUseCase(
            IUnidadRepository unidadRepository) {
        this.unidadRepository = unidadRepository;
    }

    public PaginatedResponse<List<UnidadResponse>> execute(ObtenerUnidadesRequest obtenerUnidadesRequest) {
        PaginatedResponse<List<Unidad>> resultado = unidadRepository.obtenerTodos(obtenerUnidadesRequest);

        List<UnidadResponse> unidades = resultado.getData().stream()
                .map(UnidadResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                unidades,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
