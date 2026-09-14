package com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Application.Dtos.ObtenerExistenciasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.Aggregates.Existencia;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IExistenciaRepository {
    Existencia crear(Existencia existencia);
    Existencia actualizar(ExistenciaId existenciaId, Existencia existencia);
    Optional<Existencia> obtenerPorId(ExistenciaId existenciaId);
    PaginatedResponse<List<Existencia>> obtenerTodos(ObtenerExistenciasRequest obtenerExistenciasRequest);
    void eliminar(ExistenciaId existenciaId);
}
