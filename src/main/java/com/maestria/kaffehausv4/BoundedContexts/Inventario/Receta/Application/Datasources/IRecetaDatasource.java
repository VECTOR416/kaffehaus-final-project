package com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Application.Dtos.ObtenerRecetasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.Aggregates.Receta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Receta.Domain.ValueObjects.RecetaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IRecetaDatasource {
    Receta crear(Receta receta);
    Receta actualizar(RecetaId recetaId, Receta receta);
    Optional<Receta> obtenerPorId(RecetaId recetaId);
    PaginatedResponse<List<Receta>> obtenerTodos(ObtenerRecetasRequest obtenerRecetasRequest);
    void eliminar(RecetaId recetaId);
}
