package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Datasources.IClienteDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.ObtenerClientesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

@Repository 
public class ClienteDatasourceImpl implements IClienteDatasource{

    @Override
    public Cliente crear(Cliente cliente) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'crear'");
    }

    @Override
    public Cliente actualizar(ClienteId clienteId, Cliente cliente) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'actualizar'");
    }

    @Override
    public Optional<Cliente> obtenerPorId(ClienteId clienteId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'obtenerPorId'");
    }

    @Override
    public PaginatedResponse<List<Cliente>> obtenerTodos(ObtenerClientesRequest obtenerClientesRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'obtenerTodos'");
    }

    @Override
    public void eliminar(ClienteId clienteId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'eliminar'");
    }

}
