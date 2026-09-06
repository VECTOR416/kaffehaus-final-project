package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Dtos.EntidadResponse;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Repositories.IEntidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.Aggregates.Entidad;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects.EntidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional(readOnly = true)
public class ObtenerEntidadPorIdUseCase {
    private final IEntidadRepository entidadRepository;

    public ObtenerEntidadPorIdUseCase(IEntidadRepository entidadRepository) {
        this.entidadRepository = entidadRepository;
    }

    public EntidadResponse execute(String entidadId) {
        EntidadId id = new EntidadId(entidadId);

        Entidad entidad = entidadRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La entidad con id '" + entidadId + "' no existe"));

        return EntidadResponse.createFromAggregate(entidad);
    }
}
