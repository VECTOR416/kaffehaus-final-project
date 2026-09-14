package com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Application.Dtos.ObtenerPasosRecetaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.Aggregates.PasoReceta;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.PasoReceta.Domain.ValueObjects.PasoRecetaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IPasoRecetaDatasource {
    PasoReceta crear(PasoReceta pasoReceta);
    PasoReceta actualizar(PasoRecetaId pasoRecetaId, PasoReceta pasoReceta);
    Optional<PasoReceta> obtenerPorId(PasoRecetaId pasoRecetaId);
    PaginatedResponse<List<PasoReceta>> obtenerTodos(ObtenerPasosRecetaRequest obtenerPasosRecetaRequest);
    void eliminar(PasoRecetaId pasoRecetaId);
}
