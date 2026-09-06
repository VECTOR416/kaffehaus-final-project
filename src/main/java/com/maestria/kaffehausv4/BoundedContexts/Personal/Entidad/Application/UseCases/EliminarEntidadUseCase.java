package com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.UseCases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Application.Repositories.IEntidadRepository;
import com.maestria.kaffehausv4.BoundedContexts.Personal.Entidad.Domain.ValueObjects.EntidadId;
import com.maestria.kaffehausv4.Shared.Domain.Error.DomainException;

@Service
@Transactional
public class EliminarEntidadUseCase {
    private final IEntidadRepository entidadRepository;

    public EliminarEntidadUseCase(IEntidadRepository entidadRepository) {
        this.entidadRepository = entidadRepository;
    }

    public void execute(String entidadId) {
        EntidadId id = new EntidadId(entidadId);

        entidadRepository.obtenerPorId(id)
                .orElseThrow(() -> DomainException.throwException(404, "La entidad con id '" + entidadId + "' no existe"));

        entidadRepository.eliminar(id);
    }
}
