package com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Application.Dtos.ObtenerTiposDescuentoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.Aggregates.TipoDescuento;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.TipoDescuento.Domain.ValueObjects.TipoDescuentoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface ITipoDescuentoDatasource {
    TipoDescuento crear(TipoDescuento tipoDescuento);
    TipoDescuento actualizar(TipoDescuentoId tipoDescuentoId, TipoDescuento tipoDescuento);
    Optional<TipoDescuento> obtenerPorId(TipoDescuentoId tipoDescuentoId);
    PaginatedResponse<List<TipoDescuento>> obtenerTodos(ObtenerTiposDescuentoRequest obtenerTiposDescuentoRequest);
    void eliminar(TipoDescuentoId tipoDescuentoId);
}
