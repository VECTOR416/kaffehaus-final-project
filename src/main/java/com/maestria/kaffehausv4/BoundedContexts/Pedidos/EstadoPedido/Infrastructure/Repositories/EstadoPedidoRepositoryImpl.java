package com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Datasources.IEstadoPedidoDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Dtos.ObtenerEstadosPedidoRequest;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Application.Repositories.IEstadoPedidoRepository;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.Aggregates.EstadoPedido;
import com.maestria.kaffehausv4.BoundedContexts.Pedidos.EstadoPedido.Domain.ValueObjects.EstadoPedidoId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository
public class EstadoPedidoRepositoryImpl implements IEstadoPedidoRepository{

    private final IEstadoPedidoDatasource estadoPedidoDatasource;

    public EstadoPedidoRepositoryImpl(IEstadoPedidoDatasource estadoPedidoDatasource) {
        this.estadoPedidoDatasource = estadoPedidoDatasource;
    }

    @Override
    public EstadoPedido crear(EstadoPedido estadoPedido) {
        return this.estadoPedidoDatasource.crear(estadoPedido);
    }

    @Override
    public EstadoPedido actualizar(EstadoPedidoId estadoPedidoId, EstadoPedido estadoPedido) {
        return this.estadoPedidoDatasource.actualizar(estadoPedidoId, estadoPedido);
    }

    @Override
    public Optional<EstadoPedido> obtenerPorId(EstadoPedidoId estadoPedidoId) {
        return this.estadoPedidoDatasource.obtenerPorId(estadoPedidoId);
    }

    @Override
    public PaginatedResponse<List<EstadoPedido>> obtenerTodos(ObtenerEstadosPedidoRequest obtenerEstadosPedidoRequest) {
        return this.estadoPedidoDatasource.obtenerTodos(obtenerEstadosPedidoRequest);
    }

    @Override
    public void eliminar(EstadoPedidoId estadoPedidoId) {
        this.estadoPedidoDatasource.eliminar(estadoPedidoId);
    }

    @Override
    public Optional<EstadoPedido> obtenerPorNombre(String nombre) {
        return this.estadoPedidoDatasource.obtenerPorNombre(nombre);
    }

}
