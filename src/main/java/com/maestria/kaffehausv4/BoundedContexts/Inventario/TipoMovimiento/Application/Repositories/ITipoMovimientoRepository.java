package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.ObtenerTiposMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface ITipoMovimientoRepository {
    TipoMovimiento crear(TipoMovimiento tipoMovimiento);
    TipoMovimiento actualizar(TipoMovimientoId tipoMovimientoId, TipoMovimiento tipoMovimiento);
    Optional<TipoMovimiento> obtenerPorId(TipoMovimientoId tipoMovimientoId);
    PaginatedResponse<List<TipoMovimiento>> obtenerTodos(ObtenerTiposMovimientoRequest obtenerTiposMovimientoRequest);
    void eliminar(TipoMovimientoId tipoMovimientoId);
}
