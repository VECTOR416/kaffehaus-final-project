package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Repositories.IEntidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects.EntidadId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.ValueObjects.PersonaId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.CrearProveedorRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ProveedorResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Repositories.IProveedorRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.Aggregates.Proveedor;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class CrearProveedorUseCase {
    private final IProveedorRepository proveedorRepository;
    private final IPersonaRepository personaRepository;
    private final IEntidadRepository entidadRepository;

    public CrearProveedorUseCase(
            IProveedorRepository proveedorRepository,
            IPersonaRepository personaRepository,
            IEntidadRepository entidadRepository) {
        this.proveedorRepository = proveedorRepository;
        this.personaRepository = personaRepository;
        this.entidadRepository = entidadRepository;
    }

    public ProveedorResponse execute(CrearProveedorRequest crearProveedorRequest) {
        String idPersona = crearProveedorRequest.getIdPersona();
        String idEntidad = crearProveedorRequest.getIdEntidad();

        if ((idPersona == null) == (idEntidad == null)) {
            throw DomainException.throwException(400, "Debe proporcionar exactamente uno de 'idPersona' o 'idEntidad'");
        }

        PersonaId personaId = null;
        Persona persona = null;
        EntidadId entidadId = null;
        Entidad entidad = null;

        if (idPersona != null) {
            personaId = new PersonaId(idPersona);
            persona = personaRepository.obtenerPorId(personaId)
                    .orElseThrow(() -> DomainException.throwException(404, "La persona con id '" + idPersona + "' no existe"));
        } else {
            entidadId = new EntidadId(idEntidad);
            entidad = entidadRepository.obtenerPorId(entidadId)
                    .orElseThrow(() -> DomainException.throwException(404, "La entidad con id '" + idEntidad + "' no existe"));
        }

        Proveedor proveedor = Proveedor.crear(personaId, entidadId);
        Proveedor proveedorCreado = proveedorRepository.crear(proveedor);

        return ProveedorResponse.createFromAggregate(proveedorCreado, persona, entidad);
    }
}
