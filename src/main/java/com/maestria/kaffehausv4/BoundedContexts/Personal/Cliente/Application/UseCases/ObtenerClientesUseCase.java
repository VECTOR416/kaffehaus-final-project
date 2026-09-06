package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.UseCases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.ClienteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.ObtenerClientesRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination.PaginatedResponse;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerClientesUseCase {
    private final IClienteRepository clienteRepository;
    private final IPersonaRepository personaRepository;

    public ObtenerClientesUseCase(
            IClienteRepository clienteRepository,
            IPersonaRepository personaRepository) {
        this.clienteRepository = clienteRepository;
        this.personaRepository = personaRepository;
    }

    public PaginatedResponse<List<ClienteResponse>> execute(ObtenerClientesRequest obtenerClientesRequest) {
        PaginatedResponse<List<Cliente>> resultado = clienteRepository.obtenerTodos(obtenerClientesRequest);

        List<ClienteResponse> clientes = resultado.getData().stream()
                .map(this::toClienteResponse)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                clientes,
                resultado.getSkip(),
                resultado.getLimit(),
                resultado.getTotal(),
                resultado.isHasMore()
        );
    }

    private ClienteResponse toClienteResponse(Cliente cliente) {
        Persona persona = personaRepository.obtenerPorId(cliente.getPersona())
                .orElseThrow(() -> DomainException.throwException(404, "La persona asociada al cliente no existe"));
        return ClienteResponse.createFromAggregate(cliente, persona);
    }
}
