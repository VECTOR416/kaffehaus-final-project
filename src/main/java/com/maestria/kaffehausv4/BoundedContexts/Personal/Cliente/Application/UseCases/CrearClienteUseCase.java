package com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.ClienteResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Dtos.CrearClienteRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Application.Repositories.IClienteRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Cliente.Domain.Aggregates.Cliente;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;

@Service
@Transactional
public class CrearClienteUseCase {
    private final IClienteRepository clienteRepository;
    private final IPersonaRepository personaRepository;

    public CrearClienteUseCase(
            IClienteRepository clienteRepository,
            IPersonaRepository personaRepository) {
        this.clienteRepository = clienteRepository;
        this.personaRepository = personaRepository;
    }

    public ClienteResponse execute(CrearClienteRequest crearClienteRequest) {
        String idPersona = crearClienteRequest.getIdPersona();
        Persona persona;
        if (idPersona != null) {
            PersonaId personaId = new PersonaId(idPersona);
            persona = personaRepository.obtenerPorId(personaId)
                    .map(personaDB -> {
                        personaDB.actualizar(
                                crearClienteRequest.getNombres(),
                                crearClienteRequest.getApellidos(),
                                crearClienteRequest.getTelefono(),
                                crearClienteRequest.getCarnet());
                        return personaRepository.actualizar(personaId, personaDB);
                    })
                    .orElseGet(() -> {
                        Persona personaNueva = Persona.crear(
                                crearClienteRequest.getNombres(),
                                crearClienteRequest.getApellidos(),
                                crearClienteRequest.getTelefono(),
                                crearClienteRequest.getCarnet());
                        return personaRepository.crear(personaNueva);
                    });
        } else {
            Persona personaNueva = Persona.crear(
                    crearClienteRequest.getNombres(),
                    crearClienteRequest.getApellidos(),
                    crearClienteRequest.getTelefono(),
                    crearClienteRequest.getCarnet());
            persona = personaRepository.crear(personaNueva);
        }

        Cliente cliente = Cliente.crear(persona.getId().getValue());
        Cliente clienteCreado = this.clienteRepository.crear(cliente);
        return ClienteResponse.createFromAggregate(clienteCreado, persona);
    }
}
