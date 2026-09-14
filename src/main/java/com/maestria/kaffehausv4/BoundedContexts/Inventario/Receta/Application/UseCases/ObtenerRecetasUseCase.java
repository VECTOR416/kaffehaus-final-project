package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ObtenerRecetasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.RecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Repositories.IRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerRecetasUseCase {
    private final IRecetaRepository recetaRepository;

    public ObtenerRecetasUseCase(
            IRecetaRepository recetaRepository) {
        this.recetaRepository = recetaRepository;
    }

    public PaginatedResponse<List<RecetaResponse>> execute(ObtenerRecetasRequest obtenerRecetasRequest) {
        PaginatedResponse<List<Receta>> resultado = recetaRepository.obtenerTodos(obtenerRecetasRequest);

        List<RecetaResponse> recetas = resultado.getData().stream()
                .map(RecetaResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                recetas,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
