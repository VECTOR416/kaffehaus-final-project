package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.ValueObjects.ClienteId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarClienteUseCase {
    private final IClienteRepository clienteRepository;

    public EliminarClienteUseCase(IClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public void execute(String clienteId) {
        ClienteId id = new ClienteId(clienteId);

        clienteRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El cliente no existe"));

        clienteRepository.eliminar(id);
    }
}
