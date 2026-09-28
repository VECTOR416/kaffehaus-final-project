package com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Application.Dtos.ObtenerTiposRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.Aggregates.Tipo;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Tipo.Domain.ValueObjects.TipoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface ITipoDatasource {
    Tipo crear(Tipo tipo);
    Tipo actualizar(TipoId tipoId, Tipo tipo);
    Optional<Tipo> obtenerPorId(TipoId tipoId);
    PaginatedResponse<List<Tipo>> obtenerTodos(ObtenerTiposRequest obtenerTiposRequest);
    void eliminar(TipoId tipoId);

    /** Fila viva con ese nombre, sin distinguir mayusculas. La usa la siembra para no duplicar. */
    Optional<Tipo> obtenerPorNombre(String nombre);
}
