package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.ClienteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerClientePorIdUseCase {
    private final IClienteRepository clienteRepository;
    private final IPersonaRepository personaRepository;

    public ObtenerClientePorIdUseCase(
            IClienteRepository clienteRepository,
            IPersonaRepository personaRepository) {
        this.clienteRepository = clienteRepository;
        this.personaRepository = personaRepository;
    }

    public ClienteResponse execute(String clienteId) {
        ClienteId id = new ClienteId(clienteId);

        Cliente cliente = clienteRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El cliente no existe"));

        Persona persona = personaRepository.obtenerPorId(cliente.getPersona())
                .orElseThrow(() -> DomainException.throwException(404, "La persona asociada al cliente no existe"));

        return ClienteResponse.createFromAggregate(cliente, persona);
    }
}
