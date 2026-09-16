package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Infrastructure.Datasources;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Datasources.IClienteDatasource;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.ObtenerClientesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Infrastructure.Persistence.Jpa.ClienteJpaMapper;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Infrastructure.Persistence.Jpa.ClienteJpaModel;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Infrastructure.Persistence.Jpa.ClienteJpaRepository;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;
import com.maestria.kaffehausv4.Shared.Infrastructure.Persistence.OffsetPageRequest;

@Repository 
public class ClienteDatasourceImpl implements IClienteDatasource{

    private final ClienteJpaRepository clienteJpaRepository;

    public ClienteDatasourceImpl(ClienteJpaRepository clienteJpaRepository) {
        this.clienteJpaRepository = clienteJpaRepository;
    }

    @Override
    public Cliente crear(Cliente cliente) {
        ClienteJpaModel model = ClienteJpaMapper.toPersistence(cliente);
        return ClienteJpaMapper.toDomain(clienteJpaRepository.save(model));
    }

    @Override
    public Cliente actualizar(ClienteId clienteId, Cliente cliente) {
        ClienteJpaModel model = clienteJpaRepository.findByIdAndBorradoFalse(aUuid(clienteId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El cliente con id '" + clienteId.getValue() + "' no existe"));

        ClienteJpaMapper.copiarCambios(cliente, model);

        return ClienteJpaMapper.toDomain(clienteJpaRepository.save(model));
    }

    @Override
    public Optional<Cliente> obtenerPorId(ClienteId clienteId) {
        return clienteJpaRepository.findByIdAndBorradoFalse(aUuid(clienteId.getValue()))
                .map(ClienteJpaMapper::toDomain);
    }

    @Override
    public PaginatedResponse<List<Cliente>> obtenerTodos(ObtenerClientesRequest obtenerClientesRequest) {
        int skip = obtenerClientesRequest.getSkip();
        int limit = obtenerClientesRequest.getLimit();

        Pageable pageable = new OffsetPageRequest(skip, limit);

        Page<ClienteJpaModel> pagina = clienteJpaRepository.findByBorradoFalse(pageable);

        List<Cliente> datos = pagina.getContent().stream()
                .map(ClienteJpaMapper::toDomain)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                datos,
                skip,
                limit,
                pagina.getTotalElements(),
                skip + datos.size() < pagina.getTotalElements()
        );
    }

    @Override
    public void eliminar(ClienteId clienteId) {
        ClienteJpaModel model = clienteJpaRepository.findByIdAndBorradoFalse(aUuid(clienteId.getValue()))
                .orElseThrow(() -> DomainException.throwException(404, "El cliente con id '" + clienteId.getValue() + "' no existe"));

        Cliente cliente = ClienteJpaMapper.toDomain(model);
        cliente.eliminar();

        ClienteJpaMapper.copiarCambios(cliente, model);
        clienteJpaRepository.save(model);
    }

    private static UUID aUuid(String valor) {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw DomainException.throwException(400, "El identificador '" + valor + "' no es un UUID valido");
        }
    }
}
