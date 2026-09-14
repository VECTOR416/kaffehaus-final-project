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
}
