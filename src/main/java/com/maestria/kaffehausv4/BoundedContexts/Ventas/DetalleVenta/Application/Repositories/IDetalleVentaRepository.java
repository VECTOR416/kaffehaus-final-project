package com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Application.Dtos.ObtenerDetallesVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.Aggregates.DetalleVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.DetalleVenta.Domain.ValueObjects.DetalleVentaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IDetalleVentaRepository {
    DetalleVenta crear(DetalleVenta detalleVenta);
    DetalleVenta actualizar(DetalleVentaId detalleVentaId, DetalleVenta detalleVenta);
    Optional<DetalleVenta> obtenerPorId(DetalleVentaId detalleVentaId);
    PaginatedResponse<List<DetalleVenta>> obtenerTodos(ObtenerDetallesVentaRequest obtenerDetallesVentaRequest);
    void eliminar(DetalleVentaId detalleVentaId);
}
