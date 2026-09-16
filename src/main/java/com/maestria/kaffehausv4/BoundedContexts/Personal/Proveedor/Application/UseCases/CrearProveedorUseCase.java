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

/**
 * La persona de contacto se crea aqui dentro, en la misma transaccion que el proveedor,
 * porque Persona no tiene alta propia. La entidad, en cambio, debe existir de antes: se
 * referencia por id y se valida.
 */
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
        String idEntidad = crearProveedorRequest.getIdEntidad();

        EntidadId entidadId = null;
        Entidad entidad = null;

        if (idEntidad != null) {
            entidadId = new EntidadId(idEntidad);
            entidad = entidadRepository.obtenerPorId(entidadId)
                    .orElseThrow(() -> DomainException.throwException(404, "La entidad con id '" + idEntidad + "' no existe"));
        }

        Persona persona = resolverPersona(crearProveedorRequest);

        Proveedor proveedor = Proveedor.crear(persona.getId(), entidadId);
        Proveedor proveedorCreado = proveedorRepository.crear(proveedor);

        return ProveedorResponse.createFromAggregate(proveedorCreado, persona, entidad);
    }

    private Persona resolverPersona(CrearProveedorRequest crearProveedorRequest) {
        String idPersona = crearProveedorRequest.getIdPersona();

        if (idPersona == null) {
            return personaRepository.crear(nuevaPersona(crearProveedorRequest));
        }

        PersonaId personaId = new PersonaId(idPersona);

        return personaRepository.obtenerPorId(personaId)
                .map(personaDB -> {
                    personaDB.actualizar(
                            crearProveedorRequest.getNombres(),
                            crearProveedorRequest.getApellidos(),
                            crearProveedorRequest.getTelefono(),
                            crearProveedorRequest.getCarnet());
                    return personaRepository.actualizar(personaId, personaDB);
                })
                .orElseGet(() -> personaRepository.crear(nuevaPersona(crearProveedorRequest)));
    }

    private static Persona nuevaPersona(CrearProveedorRequest crearProveedorRequest) {
        return Persona.crear(
                crearProveedorRequest.getNombres(),
                crearProveedorRequest.getApellidos(),
                crearProveedorRequest.getTelefono(),
                crearProveedorRequest.getCarnet());
    }
}
