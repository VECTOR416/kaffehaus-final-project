package com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Application.Dtos.ObtenerIngredientesRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.Aggregates.IngredienteReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.IngredienteReceta.Domain.ValueObjects.IngredienteRecetaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IIngredienteRecetaDatasource {
    IngredienteReceta crear(IngredienteReceta ingredienteReceta);
    IngredienteReceta actualizar(IngredienteRecetaId ingredienteRecetaId, IngredienteReceta ingredienteReceta);
    Optional<IngredienteReceta> obtenerPorId(IngredienteRecetaId ingredienteRecetaId);
    PaginatedResponse<List<IngredienteReceta>> obtenerTodos(ObtenerIngredientesRecetaRequest obtenerIngredientesRecetaRequest);
    void eliminar(IngredienteRecetaId ingredienteRecetaId);
}
