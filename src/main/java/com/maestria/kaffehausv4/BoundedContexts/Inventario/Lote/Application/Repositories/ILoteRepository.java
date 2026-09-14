package com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Application.Dtos.ObtenerLotesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.Aggregates.Lote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Lote.Domain.ValueObjects.LoteId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface ILoteRepository {
    Lote crear(Lote lote);
    Lote actualizar(LoteId loteId, Lote lote);
    Optional<Lote> obtenerPorId(LoteId loteId);
    PaginatedResponse<List<Lote>> obtenerTodos(ObtenerLotesRequest obtenerLotesRequest);
    void eliminar(LoteId loteId);
}
