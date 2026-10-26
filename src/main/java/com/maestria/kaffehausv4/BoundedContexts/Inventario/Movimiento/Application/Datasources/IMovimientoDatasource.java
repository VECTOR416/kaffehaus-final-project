package com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Application.Dtos.ObtenerMovimientosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.Aggregates.Movimiento;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Movimiento.Domain.ValueObjects.MovimientoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.BoundedContexts.Inventario.Existencia.Domain.ValueObjects.ExistenciaId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;

public interface IMovimientoDatasource {
    Movimiento crear(Movimiento movimiento);
    Movimiento actualizar(MovimientoId movimientoId, Movimiento movimiento);
    Optional<Movimiento> obtenerPorId(MovimientoId movimientoId);
    PaginatedResponse<List<Movimiento>> obtenerTodos(ObtenerMovimientosRequest obtenerMovimientosRequest);
    void eliminar(MovimientoId movimientoId);

    /**
     * Movimientos vivos de esas existencias. El pesaje los usa para saber cual se movio por ultima
     * vez (logica.md EXISTENCIA-16).
     */
    List<Movimiento> obtenerPorExistencias(List<ExistenciaId> existenciaIds);

    /**
     * Movimientos vivos de una venta, de cualquier tipo. El reintegro los usa para devolver lo
     * descontado a sus mismos sujetos (logica.md VENTA-25).
     */
    List<Movimiento> obtenerPorVenta(VentaId ventaId);
}
