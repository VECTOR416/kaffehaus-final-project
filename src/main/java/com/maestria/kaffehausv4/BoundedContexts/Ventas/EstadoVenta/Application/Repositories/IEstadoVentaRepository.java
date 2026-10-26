package com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Application.Dtos.ObtenerEstadosVentaRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.Aggregates.EstadoVenta;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.EstadoVenta.Domain.ValueObjects.EstadoVentaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IEstadoVentaRepository {
    EstadoVenta crear(EstadoVenta estadoVenta);
    EstadoVenta actualizar(EstadoVentaId estadoVentaId, EstadoVenta estadoVenta);
    Optional<EstadoVenta> obtenerPorId(EstadoVentaId estadoVentaId);
    PaginatedResponse<List<EstadoVenta>> obtenerTodos(ObtenerEstadosVentaRequest obtenerEstadosVentaRequest);
    void eliminar(EstadoVentaId estadoVentaId);

    /**
     * Busca la fila viva por nombre, sin distinguir mayusculas. El flujo de ventas y pedidos
     * resuelve por nombre las filas de catalogo que necesita (ventas.md VEN-03, VEN-51), igual
     * que la seguridad resuelve los roles: el catalogo sigue siendo datos, no un enum.
     */
    Optional<EstadoVenta> obtenerPorNombre(String nombre);
}
