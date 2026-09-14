package com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Application.Dtos.ObtenerUnidadesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.Aggregates.Unidad;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Unidad.Domain.ValueObjects.UnidadId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IUnidadRepository {
    Unidad crear(Unidad unidad);
    Unidad actualizar(UnidadId unidadId, Unidad unidad);
    Optional<Unidad> obtenerPorId(UnidadId unidadId);
    PaginatedResponse<List<Unidad>> obtenerTodos(ObtenerUnidadesRequest obtenerUnidadesRequest);
    void eliminar(UnidadId unidadId);
}
