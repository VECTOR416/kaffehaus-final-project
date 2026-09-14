package com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Application.Dtos.ObtenerDetallesLoteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.Aggregates.DetalleLote;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.DetalleLote.Domain.ValueObjects.DetalleLoteId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IDetalleLoteDatasource {
    DetalleLote crear(DetalleLote detalleLote);
    DetalleLote actualizar(DetalleLoteId detalleLoteId, DetalleLote detalleLote);
    Optional<DetalleLote> obtenerPorId(DetalleLoteId detalleLoteId);
    PaginatedResponse<List<DetalleLote>> obtenerTodos(ObtenerDetallesLoteRequest obtenerDetallesLoteRequest);
    void eliminar(DetalleLoteId detalleLoteId);
}
