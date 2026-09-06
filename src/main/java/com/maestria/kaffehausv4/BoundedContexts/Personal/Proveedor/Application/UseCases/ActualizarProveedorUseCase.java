package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Repositories.IEntidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects.EntidadId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ActualizarProveedorRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ProveedorResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Repositories.IProveedorRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.Aggregates.Proveedor;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class ActualizarProveedorUseCase {
    private final IProveedorRepository proveedorRepository;
    private final IPersonaRepository personaRepository;
    private final IEntidadRepository entidadRepository;

    public ActualizarProveedorUseCase(
            IProveedorRepository proveedorRepository,
            IPersonaRepository personaRepository,
            IEntidadRepository entidadRepository) {
        this.proveedorRepository = proveedorRepository;
        this.personaRepository = personaRepository;
        this.entidadRepository = entidadRepository;
    }

    public ProveedorResponse execute(String proveedorId, ActualizarProveedorRequest actualizarProveedorRequest) {
        ProveedorId id = new ProveedorId(proveedorId);

        Proveedor proveedor = proveedorRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "El proveedor con id '" + proveedorId + "' no existe"));

        String idPersona = actualizarProveedorRequest.getIdPersona();
        String idEntidad = actualizarProveedorRequest.getIdEntidad();

        if (idPersona != null && idEntidad != null) {
            throw DomainException.throwException(400, "Un proveedor no puede estar vinculado a una persona y a una entidad simultaneamente");
        }

        if (idPersona != null) {
            PersonaId personaId = new PersonaId(idPersona);
            personaRepository.obtenerPorId(personaId)
                    .orElseThrow(() -> DomainException.throwException(404, "La persona con id '" + idPersona + "' no existe"));
            proveedor.setPersonaId(personaId);
            proveedor.setEntidadId(null);
        } else if (idEntidad != null) {
            EntidadId entidadId = new EntidadId(idEntidad);
            entidadRepository.obtenerPorId(entidadId)
                    .orElseThrow(() -> DomainException.throwException(404, "La entidad con id '" + idEntidad + "' no existe"));
            proveedor.setEntidadId(entidadId);
            proveedor.setPersonaId(null);
        }

        Proveedor proveedorActualizado = proveedorRepository.actualizar(id, proveedor);

        Persona persona = proveedorActualizado.getPersonaId() != null
                ? personaRepository.obtenerPorId(proveedorActualizado.getPersonaId())
                        .orElseThrow(() -> DomainException.throwException(404, "La persona asociada al proveedor no existe"))
                : null;
        Entidad entidad = proveedorActualizado.getEntidadId() != null
                ? entidadRepository.obtenerPorId(proveedorActualizado.getEntidadId())
                        .orElseThrow(() -> DomainException.throwException(404, "La entidad asociada al proveedor no existe"))
                : null;

        return ProveedorResponse.createFromAggregate(proveedorActualizado, persona, entidad);
    }
}
