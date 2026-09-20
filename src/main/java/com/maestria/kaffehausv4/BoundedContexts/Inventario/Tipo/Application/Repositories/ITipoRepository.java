package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.ObtenerTiposRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface ITipoRepository {
    Tipo crear(Tipo tipo);
    Tipo actualizar(TipoId tipoId, Tipo tipo);
    Optional<Tipo> obtenerPorId(TipoId tipoId);
    PaginatedResponse<List<Tipo>> obtenerTodos(ObtenerTiposRequest obtenerTiposRequest);
    void eliminar(TipoId tipoId);
}
