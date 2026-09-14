package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Datasources;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.ObtenerPedidosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.Aggregates.Pedido;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.ValueObjects.PedidoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IPedidoDatasource {
    Pedido crear(Pedido pedido);
    Pedido actualizar(PedidoId pedidoId, Pedido pedido);
    Optional<Pedido> obtenerPorId(PedidoId pedidoId);
    PaginatedResponse<List<Pedido>> obtenerTodos(ObtenerPedidosRequest obtenerPedidosRequest);
    void eliminar(PedidoId pedidoId);
}
