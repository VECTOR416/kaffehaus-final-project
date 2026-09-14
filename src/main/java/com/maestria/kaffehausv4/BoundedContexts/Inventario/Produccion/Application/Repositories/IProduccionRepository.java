package com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Application.Dtos.ObtenerProduccionesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.Aggregates.Produccion;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Produccion.Domain.ValueObjects.ProduccionId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IProduccionRepository {
    Produccion crear(Produccion produccion);
    Produccion actualizar(ProduccionId produccionId, Produccion produccion);
    Optional<Produccion> obtenerPorId(ProduccionId produccionId);
    PaginatedResponse<List<Produccion>> obtenerTodos(ObtenerProduccionesRequest obtenerProduccionesRequest);
    void eliminar(ProduccionId produccionId);
}
