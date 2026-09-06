package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.ActualizarEntidadRequest;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.EntidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Repositories.IEntidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects.EntidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;



@Service
@Transactional
public class ActualizarEntidadUseCase {
    private final IEntidadRepository entidadRepository;

    public ActualizarEntidadUseCase(IEntidadRepository entidadRepository) {
        this.entidadRepository = entidadRepository;
    }

    public EntidadResponse execute(String entidadId, ActualizarEntidadRequest actualizarEntidadRequest) {
        EntidadId id = new EntidadId(entidadId);

        Entidad entidad = entidadRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La entidad con id '" + entidadId + "' no existe"));

        entidad.actualizar(
                actualizarEntidadRequest.getNombre(),
                actualizarEntidadRequest.getNit(),
                actualizarEntidadRequest.getTelefono(),
                actualizarEntidadRequest.getDescripcion());

        Entidad entidadActualizada = entidadRepository.actualizar(id, entidad);

        return EntidadResponse.createFromAggregate(entidadActualizada);
    }
}
