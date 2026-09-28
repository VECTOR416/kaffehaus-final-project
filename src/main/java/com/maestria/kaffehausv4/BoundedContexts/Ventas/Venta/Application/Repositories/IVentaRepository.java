package com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Application.Dtos.ObtenerVentasRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.Aggregates.Venta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IVentaRepository {
    Venta crear(Venta venta);
    Venta actualizar(VentaId ventaId, Venta venta);
    Optional<Venta> obtenerPorId(VentaId ventaId);
    PaginatedResponse<List<Venta>> obtenerTodos(ObtenerVentasRequest obtenerVentasRequest);
    void eliminar(VentaId ventaId);

    /** Venta registrada con esa clave de idempotencia, si la hay (ventas.md VEN-66). */
    Optional<Venta> obtenerPorClaveIdempotencia(String claveIdempotencia);
}
