package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.ObtenerEstadosVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IEstadoVentaDatasource {
    EstadoVenta crear(EstadoVenta estadoVenta);
    EstadoVenta actualizar(EstadoVentaId estadoVentaId, EstadoVenta estadoVenta);
    Optional<EstadoVenta> obtenerPorId(EstadoVentaId estadoVentaId);
    PaginatedResponse<List<EstadoVenta>> obtenerTodos(ObtenerEstadosVentaRequest obtenerEstadosVentaRequest);
    void eliminar(EstadoVentaId estadoVentaId);
}
