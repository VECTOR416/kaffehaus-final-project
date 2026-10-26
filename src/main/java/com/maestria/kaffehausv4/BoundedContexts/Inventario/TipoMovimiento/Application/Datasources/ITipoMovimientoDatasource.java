package com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Application.Dtos.ObtenerTiposMovimientoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.Aggregates.TipoMovimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.TipoMovimiento.Domain.ValueObjects.TipoMovimientoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface ITipoMovimientoDatasource {
    TipoMovimiento crear(TipoMovimiento tipoMovimiento);
    TipoMovimiento actualizar(TipoMovimientoId tipoMovimientoId, TipoMovimiento tipoMovimiento);
    Optional<TipoMovimiento> obtenerPorId(TipoMovimientoId tipoMovimientoId);
    PaginatedResponse<List<TipoMovimiento>> obtenerTodos(ObtenerTiposMovimientoRequest obtenerTiposMovimientoRequest);
    void eliminar(TipoMovimientoId tipoMovimientoId);

    /**
     * Busca la fila viva por nombre, sin distinguir mayusculas. Existe porque el flujo de
     * inventario (produccion y venta) resuelve por nombre las filas de catalogo que necesita,
     * igual que la seguridad resuelve los roles: el catalogo sigue siendo datos, no un enum.
     */
    Optional<TipoMovimiento> obtenerPorNombre(String nombre);
}
