package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories;

import java.util.List;
import java.util.Optional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.ObtenerClientesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;

public interface IClienteRepository {
    Cliente crear(Cliente cliente);
    Cliente actualizar(ClienteId clienteId, Cliente cliente);
    Optional<Cliente> obtenerPorId(ClienteId clienteId);
    PaginatedResponse<List<Cliente>> obtenerTodos(ObtenerClientesRequest obtenerClientesRequest);
    void eliminar(ClienteId clienteId);
}
