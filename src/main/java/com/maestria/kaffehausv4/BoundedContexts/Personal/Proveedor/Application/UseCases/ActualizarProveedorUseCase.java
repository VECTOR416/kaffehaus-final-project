package com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Repositories.IEntidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects.EntidadId;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Application.Repositories.IPersonaRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Persona.Domain.Aggregates.Persona;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ActualizarProveedorRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Dtos.ProveedorResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Application.Repositories.IProveedorRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.Aggregates.Proveedor;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Proveedor.Domain.ValueObjects.ProveedorId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

/**
 * Los datos de la persona de contacto se actualizan en sitio, en la misma transaccion.
 * La entidad si se puede reapuntar por id, y enviar idEntidad nulo la deja como estaba.
 */
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

        Persona persona = personaRepository.obtenerPorId(proveedor.getPersonaId())
                .orElseThrow(() -> DomainException.throwException(404, "La persona asociada al proveedor no existe"));

        persona.actualizar(
                actualizarProveedorRequest.getNombres(),
                actualizarProveedorRequest.getApellidos(),
                actualizarProveedorRequest.getTelefono(),
                actualizarProveedorRequest.getCarnet());

        Persona personaActualizada = personaRepository.actualizar(proveedor.getPersonaId(), persona);

        String idEntidad = actualizarProveedorRequest.getIdEntidad();
        Entidad entidad = null;

        if (idEntidad != null) {
            EntidadId entidadId = new EntidadId(idEntidad);
            entidad = entidadRepository.obtenerPorId(entidadId)
                    .orElseThrow(() -> DomainException.throwException(404, "La entidad con id '" + idEntidad + "' no existe"));
            proveedor.setEntidadId(entidadId);
        } else if (proveedor.getEntidadId() != null) {
            entidad = entidadRepository.obtenerPorId(proveedor.getEntidadId())
                    .orElseThrow(() -> DomainException.throwException(404, "La entidad asociada al proveedor no existe"));
        }

        Proveedor proveedorActualizado = proveedorRepository.actualizar(id, proveedor);

        return ProveedorResponse.createFromAggregate(proveedorActualizado, personaActualizada, entidad);
    }
}
