package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Infrastructure.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Datasources.IClienteDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.ObtenerClientesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository 
public class ClienteRepositoryImpl implements IClienteRepository{

    private final IClienteDatasource clienteDatasource;

    public ClienteRepositoryImpl(IClienteDatasource clienteDatasource) {
        this.clienteDatasource = clienteDatasource;
    }

    @Override
    public Cliente crear(Cliente cliente) {
        return this.clienteDatasource.crear(cliente);
    }

    @Override
    public Cliente actualizar(ClienteId clienteId, Cliente cliente) {
        return this.clienteDatasource.actualizar(clienteId, cliente);
    }

    @Override
    public Optional<Cliente> obtenerPorId(ClienteId clienteId) {
        return this.clienteDatasource.obtenerPorId(clienteId);
    }

    @Override
    public PaginatedResponse<List<Cliente>> obtenerTodos(ObtenerClientesRequest obtenerClientesRequest) {
        return this.clienteDatasource.obtenerTodos(obtenerClientesRequest);
    }

    @Override
    public void eliminar(ClienteId clienteId) {
        this.clienteDatasource.eliminar(clienteId);
    }

}
