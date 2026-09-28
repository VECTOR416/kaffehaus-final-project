package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos.ObtenerEstadosPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IEstadoPedidoDatasource {
    EstadoPedido crear(EstadoPedido estadoPedido);
    EstadoPedido actualizar(EstadoPedidoId estadoPedidoId, EstadoPedido estadoPedido);
    Optional<EstadoPedido> obtenerPorId(EstadoPedidoId estadoPedidoId);
    PaginatedResponse<List<EstadoPedido>> obtenerTodos(ObtenerEstadosPedidoRequest obtenerEstadosPedidoRequest);
    void eliminar(EstadoPedidoId estadoPedidoId);

    /**
     * Busca la fila viva por nombre, sin distinguir mayusculas. El flujo de ventas y pedidos
     * resuelve por nombre las filas de catalogo que necesita (ventas.md VEN-03, VEN-51), igual
     * que la seguridad resuelve los roles: el catalogo sigue siendo datos, no un enum.
     */
    Optional<EstadoPedido> obtenerPorNombre(String nombre);
}
