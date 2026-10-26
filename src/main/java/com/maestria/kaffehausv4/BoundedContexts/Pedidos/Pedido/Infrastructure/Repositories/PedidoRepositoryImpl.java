package com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Datasources.IPedidoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Dtos.ObtenerPedidosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Application.Repositories.IPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.Aggregates.Pedido;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.Pedido.Domain.ValueObjects.PedidoId;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Venta.Domain.ValueObjects.VentaId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class PedidoRepositoryImpl implements IPedidoRepository{

    private final IPedidoDatasource pedidoDatasource;

    public PedidoRepositoryImpl(IPedidoDatasource pedidoDatasource) {
        this.pedidoDatasource = pedidoDatasource;
    }

    @Override
    public Pedido crear(Pedido pedido) {
        return this.pedidoDatasource.crear(pedido);
    }

    @Override
    public Pedido actualizar(PedidoId pedidoId, Pedido pedido) {
        return this.pedidoDatasource.actualizar(pedidoId, pedido);
    }

    @Override
    public Optional<Pedido> obtenerPorId(PedidoId pedidoId) {
        return this.pedidoDatasource.obtenerPorId(pedidoId);
    }

    @Override
    public PaginatedResponse<List<Pedido>> obtenerTodos(ObtenerPedidosRequest obtenerPedidosRequest) {
        return this.pedidoDatasource.obtenerTodos(obtenerPedidosRequest);
    }

    @Override
    public void eliminar(PedidoId pedidoId) {
        this.pedidoDatasource.eliminar(pedidoId);
    }

    @Override
    public Optional<Pedido> obtenerPorVentaId(VentaId ventaId) {
        return this.pedidoDatasource.obtenerPorVentaId(ventaId);
    }

}
