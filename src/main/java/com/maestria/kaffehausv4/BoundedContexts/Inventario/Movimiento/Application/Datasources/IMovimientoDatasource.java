package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.ObtenerMovimientosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.ValueObjects.MovimientoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IMovimientoDatasource {
    Movimiento crear(Movimiento movimiento);
    Movimiento actualizar(MovimientoId movimientoId, Movimiento movimiento);
    Optional<Movimiento> obtenerPorId(MovimientoId movimientoId);
    PaginatedResponse<List<Movimiento>> obtenerTodos(ObtenerMovimientosRequest obtenerMovimientosRequest);
    void eliminar(MovimientoId movimientoId);

    /**
     * Movimientos vivos con ese motivo exacto. Es como se reconocen los de una venta, que no
     * guardan id_detalle_venta y citan la venta en su motivo (inventario.md INV-33).
     */
    List<Movimiento> obtenerPorMotivo(String motivo);
}
