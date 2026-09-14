package com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.Datasources.IPedidoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.Dtos.ObtenerPedidosRequest;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Application.Repositories.IPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Domain.Aggregates.Pedido;
import com.maestria.kaffehausv4.BoundedContexts.Ventas.Pedido.Domain.ValueObjects.PedidoId;
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

}
