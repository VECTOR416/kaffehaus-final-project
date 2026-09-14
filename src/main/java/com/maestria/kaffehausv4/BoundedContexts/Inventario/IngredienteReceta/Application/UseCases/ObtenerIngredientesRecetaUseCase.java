package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos.IngredienteRecetaResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos.ObtenerIngredientesRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Repositories.IIngredienteRecetaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Service
@Transactional(readOnly = true)
public class ObtenerIngredientesRecetaUseCase {
    private final IIngredienteRecetaRepository ingredienteRecetaRepository;

    public ObtenerIngredientesRecetaUseCase(
            IIngredienteRecetaRepository ingredienteRecetaRepository) {
        this.ingredienteRecetaRepository = ingredienteRecetaRepository;
    }

    public PaginatedResponse<List<IngredienteRecetaResponse>> execute(ObtenerIngredientesRecetaRequest obtenerIngredientesRecetaRequest) {
        PaginatedResponse<List<IngredienteReceta>> resultado = ingredienteRecetaRepository.obtenerTodos(obtenerIngredientesRecetaRequest);

        List<IngredienteRecetaResponse> ingredientesReceta = resultado.getData().stream()
                .map(IngredienteRecetaResponse::createFromAggregate)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                ingredientesReceta,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }
}
